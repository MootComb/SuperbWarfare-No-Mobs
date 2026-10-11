package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.block.TripodBlock
import com.atsuishio.superbwarfare.capability.player.PlayerDeployState
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

object DeployedWeaponHandler {

    const val PIVOT_HEIGHT = 0.65625

    const val STAND_BEHIND = 1.0

    const val PITCH_LIMIT_UP = 45f

    const val PITCH_LIMIT_DOWN = 30f

    const val MAX_DRIFT = 1.0

    const val MUZZLE_CLEARANCE = 0.25

    const val FLOOR_PROBE = 0.05

    const val MAX_SINK = 0.05

    @JvmStatic
    fun clampPitch(xRot: Float): Float = Mth.clamp(xRot, -PITCH_LIMIT_UP, PITCH_LIMIT_DOWN)

    @JvmStatic
    fun standBehind(player: Player): Double {
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return STAND_BEHIND
        return GunData.from(stack).get(GunProp.STAND_BEHIND)
    }

    @JvmStatic
    fun orbitRadius(xRot: Float, standBehind: Double): Double =
        standBehind * Mth.cos(xRot * Mth.DEG_TO_RAD)

    @JvmStatic
    fun isDeployed(player: Player): Boolean = tripodOf(player) != null

    @JvmStatic
    fun tripodOf(player: Player): BlockPos? = PlayerDeployState.getOrDefault(player).tripodPos

    @JvmStatic
    fun pivotAt(tripodPos: BlockPos): Vec3 =
        Vec3(tripodPos.x + 0.5, tripodPos.y + PIVOT_HEIGHT, tripodPos.z + 0.5)

    @JvmStatic
    fun pivotOf(player: Player): Vec3? = tripodOf(player)?.let { pivotAt(it) }

    @JvmStatic
    fun deployedMuzzle(player: Player): Vec3? = pivotOf(player)?.add(0.0, MUZZLE_CLEARANCE, 0.0)

    @JvmStatic
    fun orbitPos(player: Player, tripod: BlockPos, xRot: Float): Vec3 {
        val pivot = pivotAt(tripod)
        val forward = horizontalForward(player)
        val radius = orbitRadius(xRot, standBehind(player))
        return Vec3(pivot.x - forward.x * radius, pivot.y - PIVOT_HEIGHT, pivot.z - forward.z * radius)
    }

    private fun boxAt(player: Player, pos: Vec3) =
        player.boundingBox.move(pos.x - player.x, pos.y - player.y, pos.z - player.z)

    @JvmStatic
    fun hasFloor(player: Player, pos: Vec3): Boolean =
        !player.level().noCollision(player, boxAt(player, pos).move(0.0, -FLOOR_PROBE, 0.0))

    @JvmStatic
    fun hasRoom(player: Player, pos: Vec3): Boolean =
        player.level().noCollision(player, boxAt(player, pos))

    @JvmStatic
    fun sunkTooDeep(player: Player, pos: Vec3): Boolean =
        !player.level().noCollision(player, boxAt(player, pos).deflate(MAX_SINK))

    @JvmStatic
    fun deployTarget(player: Player): BlockPos? {
        val stack = player.mainHandItem
        val item = stack.item as? GunItem ?: return null
        if (!item.deployable(GunData.from(stack))) return null
        if (player.isSpectator || player.isPassenger || !player.isAlive) return null

        val tripod = lookingTripod(player) ?: return null
        if (player.level().players().any { it !== player && tripodOf(it) == tripod }) return null
        val target = orbitPos(player, tripod, clampPitch(player.xRot))
        if (!hasFloor(player, target) || !hasRoom(player, target)) return null
        return tripod
    }

    @JvmStatic
    fun deploy(player: Player): Boolean {
        if (player.level().isClientSide) return false

        val tripod = deployTarget(player) ?: return false
        val data = GunData.from(player.mainHandItem)

        val target = orbitPos(player, tripod, clampPitch(player.xRot))

        player.teleportTo(target.x, target.y, target.z)
        player.deltaMovement = Vec3.ZERO

        val uuid = data.uuid?.toString()
        PlayerDeployState.modify(player) {
            it.tripodPos = tripod
            it.gunUuid = uuid
        }
        return true
    }

    @JvmStatic
    fun lookingTripod(player: Player): BlockPos? {
        val level = player.level()
        val start = player.eyePosition
        val end = start.add(player.getViewVector(1f).scale(player.getBlockReach()))
        val hit = level.clip(ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player))

        return hit.blockPos.takeIf { level.getBlockState(it).block is TripodBlock }
    }

    @JvmStatic
    fun undeploy(player: Player) {
        if (player.level().isClientSide) return

        PlayerDeployState.modify(player) {
            it.tripodPos = null
            it.gunUuid = null
        }
    }

    @JvmStatic
    fun applyOrbit(player: Player) {
        val pivot = pivotOf(player) ?: return

        val forward = horizontalForward(player)
        val radius = orbitRadius(player.xRot, standBehind(player))
        val x = pivot.x - forward.x * radius
        val z = pivot.z - forward.z * radius
        val y = player.y

        if (sunkTooDeep(player, Vec3(x, y, z))) return

        player.setPos(x, y, z)

        player.xo = x
        player.yo = y
        player.zo = z
        player.xOld = x
        player.yOld = y
        player.zOld = z
    }

    fun serverTick(player: Player) {
        if (player.level().isClientSide) return

        val cap = PlayerDeployState.getOrDefault(player)
        val tripod = cap.tripodPos ?: return

        val stack = player.mainHandItem
        val item = stack.item as? GunItem
        val data = if (item == null) null else GunData.from(stack)
        val stillHolding = item != null && data != null && item.deployable(data) &&
                cap.gunUuid != null && cap.gunUuid == data.uuid?.toString()

        if (!stillHolding || !player.isAlive || player.isPassenger || player.isSpectator ||
            player.level().getBlockState(tripod).block !is TripodBlock
        ) {
            undeploy(player)
            return
        }

        val pivot = pivotAt(tripod)

        val clamped = clampPitch(player.xRot)
        if (clamped != player.xRot) {
            player.xRot = clamped
            player.xRotO = clamped
        }

        val dx = player.x - pivot.x
        val dz = player.z - pivot.z
        val radius = sqrt(dx * dx + dz * dz)
        val dy = player.y - (pivot.y - PIVOT_HEIGHT)

        if (abs(radius - orbitRadius(clamped, standBehind(player))) > MAX_DRIFT ||
            abs(dy) > MAX_DRIFT ||
            !hasFloor(player, player.position()) ||
            sunkTooDeep(player, orbitPos(player, tripod, clamped)) ||
            player.level().getBlockState(player.blockPosition()).canOcclude()
        ) {
            undeploy(player)
            return
        }

        player.deltaMovement = Vec3.ZERO
        player.fallDistance = 0f
    }

    fun clientTick(player: Player) {
        if (!isDeployed(player)) return

        val clamped = clampPitch(player.xRot)
        if (clamped != player.xRot) {
            player.xRot = clamped
            player.xRotO = clamped
        }

        applyOrbit(player)
        player.deltaMovement = Vec3.ZERO

        if (player.isSprinting) {
            player.setSprinting(false)
        }
    }

    @JvmStatic
    fun horizontalForward(player: Player): Vec3 =
        Vec3.directionFromRotation(0f, player.yRot)
}
