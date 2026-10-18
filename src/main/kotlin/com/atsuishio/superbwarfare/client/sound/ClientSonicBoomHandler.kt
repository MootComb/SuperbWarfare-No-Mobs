package com.atsuishio.superbwarfare.client.sound

import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.localPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*

@OnlyIn(Dist.CLIENT)
object ClientSonicBoomHandler {

    /**
     * 掠过判定半径（格）：飞行线段与玩家眼睛的最近距离小于它就算「掠过」。
     */
    private const val NEAR_RADIUS = 5.0

    /**
     * 音量随掠过距离的衰减比例：0 = 完全不变，1 = 在 [NEAR_RADIUS] 边缘静音。
     */
    private const val NEAR_VOLUME_FALLOFF = 0.35f

    /**
     * 客户端移除时最多补算多少 tick 的位移。
     */
    private const val MAX_LOOKAHEAD_TICKS = 4.0

    class Tier(
        val minSpeed: Double,
        val sound: () -> SoundEvent,
        val volume: Float = 1f,
        val pitchJitter: Float = 0.05f,
        val cooldownTicks: Int = 0,
        val filter: (Entity) -> Boolean = { true },
    ) {
        fun matches(entity: Entity, speedSqr: Double): Boolean =
            speedSqr > minSpeed * minSpeed && filter(entity)
    }

    private val tiers: List<Tier> = listOf(
        Tier(minSpeed = 24.0, sound = { ModSounds.SNIPER_SONIC_BOOM.get() }),

        // 普通子弹（步枪 / 机枪 / SMG 一类，速度 > 15 格/tick）。
        // cooldownTicks = 1 只是把同一 tick 的多发弹丸（霰弹、分裂弹）合成一声，连发武器不受影响。
        Tier(
            minSpeed = 15.0,
            sound = { ModSounds.BULLET_SONIC_BOOM.get() },
            volume = 1.0f,
            pitchJitter = 0.08f,
            cooldownTicks = 1,
        ),
    ).sortedByDescending { it.minSpeed }

    private val played: MutableSet<Entity> = Collections.newSetFromMap(WeakHashMap())
    private var lastPlayTick = -1L

    @JvmStatic
    fun handleProjectileTick(projectile: Entity) {
        if (!DisplayConfig.ENABLE_BULLET_SONIC_BOOM.get()) return
        if (projectile in played) return

        val player = localPlayer ?: return
        if (isOwnShot(projectile, player)) return

        val from = Vec3(projectile.xo, projectile.yo, projectile.zo)
        val to = projectile.position()
        tryPlay(projectile, player, from.distanceToSqr(to), from, to)
    }

    @JvmStatic
    fun handleProjectileRemoved(projectile: Entity) {
        if (!DisplayConfig.ENABLE_BULLET_SONIC_BOOM.get()) return
        if (projectile in played) return

        val player = localPlayer ?: return
        val level = clientLevel ?: return
        if (projectile.level() !== level) return
        if (isOwnShot(projectile, player)) return

        val speedSqr = projectile.deltaMovement.lengthSqr()
        if (speedSqr <= 0.0) return

        val from = projectile.position()
        val to = level.clip(
            ClipContext(
                from,
                from.add(projectile.deltaMovement.scale(MAX_LOOKAHEAD_TICKS)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                projectile
            )
        ).location
        tryPlay(projectile, player, speedSqr, from, to)
    }

    private fun isOwnShot(projectile: Entity, player: Player): Boolean {
        val owner = (projectile as? Projectile)?.owner ?: return false
        return owner === player || owner === player.rootVehicle
    }

    private fun tryPlay(projectile: Entity, player: Player, speedSqr: Double, from: Vec3, to: Vec3) {
        val tier = tiers.firstOrNull { it.matches(projectile, speedSqr) } ?: return

        val level = clientLevel ?: return
        if (tier.cooldownTicks > 0 && lastPlayTick >= 0 && level.gameTime - lastPlayTick < tier.cooldownTicks) return

        val dir = to.subtract(from)
        val lenSqr = dir.lengthSqr()
        if (lenSqr <= 0.0) return

        val eye = player.eyePosition
        val t = (eye.subtract(from).dot(dir) / lenSqr).coerceIn(0.0, 1.0)
        val closest = from.add(dir.scale(t))
        val distance = closest.distanceTo(eye)
        if (distance > NEAR_RADIUS) return

        played += projectile
        lastPlayTick = level.gameTime

        val volume = tier.volume * (1f - (distance / NEAR_RADIUS).toFloat() * NEAR_VOLUME_FALLOFF)
        val pitch = 1f + (level.random.nextFloat() * 2f - 1f) * tier.pitchJitter
        level.playLocalSound(
            closest.x, closest.y, closest.z,
            tier.sound(), SoundSource.PLAYERS,
            volume, pitch,
            false
        )
    }
}
