package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ReforgingLightningArcMessage
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketToTrackingThis
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.network.NetworkHooks

open class ReforgingLightningBallEntity(type: EntityType<out ReforgingLightningBallEntity>, level: Level) :
    Projectile(type, level), IBulletProperties, BasicGeoProjectileEntity {

    private val animation = if (level.isClientSide) BasicProjectileAnimationInstance(this, true) else null

    override fun getAnimationInstance() = animation

    private var damage = 6f
    private var life = 100
    private var age = 0
    private var attackRadius = 8.0
    private var attackInterval = 10
    private var maxTargets = 4

    // Populated only by server-to-client discharge notifications; never used for damage.
    val lightningArcs = mutableListOf<LightningArc>()

    class LightningArc(val end: Vec3, val seed: Long, var ticks: Int = 4)

    init {
        noPhysics = true
        isNoGravity = true
        noCulling = true
    }

    override fun defineSynchedData() {}

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> =
        NetworkHooks.getEntitySpawningPacket(this)

    override fun getDamage() = damage
    override fun setDamage(value: Float) {
        damage = value.coerceAtLeast(0f)
    }

    override fun getLife() = life
    override fun setLife(value: Int) {
        life = value.coerceAtLeast(1)
    }

    override fun isPenetrating() = true
    override fun getUnderwaterMotionScale() = 1f

    @Deprecated("Deprecated in Java")
    override fun isPushedByFluid() = false

    override fun tick() {
        super.tick()
        age++
        if (!level().isClientSide && age >= life) {
            discard()
            return
        }

        // Deliberately bypass move(), ray tracing, gravity and fluid drag.
        setPos(position().add(deltaMovement))

        if (level().isClientSide) {
            lightningArcs.removeAll { --it.ticks <= 0 }
            if (tickCount % 2 == 0) {
                level().addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    x + (random.nextDouble() - 0.5) * 1.8,
                    y + (random.nextDouble() - 0.5) * 1.8,
                    z + (random.nextDouble() - 0.5) * 1.8,
                    0.0, 0.0, 0.0
                )
            }
        } else if (age % attackInterval == 0) {
            discharge()
        }
    }

    private fun discharge() {
        val shooter = owner
        val targets = level().getEntitiesOfClass(
            LivingEntity::class.java, boundingBox.inflate(attackRadius)
        ) { target ->
            target.isAlive && !target.isSpectator && target !== shooter
                    && !(target is Player && target.isCreative)
                    && !SeekTool.IS_FRIENDLY.test(shooter, target)
                    && (shooter == null || !shooter.isPassengerOfSameVehicle(target))
                    && target.boundingBox.getCenter().distanceToSqr(position()) <= attackRadius * attackRadius
        }.sortedBy { it.distanceToSqr(this) }.take(maxTargets)

        if (targets.isEmpty()) return
        val source = ModDamageTypes.causeShockDamage(level().registryAccess(), this, shooter)
        for (target in targets) {
            target.forceHurt(source, damage)
            val end = target.boundingBox.getCenter()
            sendPacketToTrackingThis(ReforgingLightningArcMessage(id, end.x, end.y, end.z, random.nextLong()))
        }
        level().playSound(null, x, y, z, ModSounds.ELECTRIC.get(), SoundSource.PLAYERS, 0.6f, 1.25f)
    }

    fun addLightningArc(end: Vec3, seed: Long) {
        if (level().isClientSide) lightningArcs.add(LightningArc(end, seed))
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        isNoGravity = true
        if (tag.contains("Damage")) setDamage(tag.getFloat("Damage"))
        if (tag.contains("Life")) setLife(tag.getInt("Life"))
        if (tag.contains("Age")) age = tag.getInt("Age").coerceAtLeast(0)
        if (tag.contains("AttackRadius")) attackRadius = tag.getDouble("AttackRadius").coerceIn(0.5, 32.0)
        if (tag.contains("AttackInterval")) attackInterval = tag.getInt("AttackInterval").coerceAtLeast(1)
        if (tag.contains("MaxTargets")) maxTargets = tag.getInt("MaxTargets").coerceIn(1, 32)
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putFloat("Damage", damage)
        tag.putInt("Life", life)
        tag.putInt("Age", age)
        tag.putDouble("AttackRadius", attackRadius)
        tag.putInt("AttackInterval", attackInterval)
        tag.putInt("MaxTargets", maxTargets)
    }
}
