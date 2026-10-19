package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.multipartRoot
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.OBB
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.EnderDragonPart
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.entity.PartEntity
import net.minecraftforge.network.NetworkHooks
import kotlin.math.ceil
import kotlin.math.sqrt

open class LaserBeamEntity(type: EntityType<out LaserBeamEntity>, level: Level) :
    Projectile(type, level), IBulletProperties {

    private var damageValue = 0f
    private var headShotMultiplier = 1f
    private var fired = false
    var power by POWER
        private set
    var startTime by START_TIME
        private set
    var ownerId by OWNER_ID
        private set
    private var endX by END_X
    private var endY by END_Y
    private var endZ by END_Z
    private var visualLife by LIFE
    var hitBlock by HIT_BLOCK
        private set

    val beamEnd: Vec3 get() = position().add(endX.toDouble(), endY.toDouble(), endZ.toDouble())
    val beamRadius: Double get() = radiusForPower(power.toDouble())

    init {
        noPhysics = true
        isNoGravity = true
        noCulling = true
    }

    override fun defineSynchedData() {
        entityData.define(POWER, 1f)
        entityData.define(START_TIME, 0L)
        entityData.define(OWNER_ID, -1)
        entityData.define(END_X, 0f)
        entityData.define(END_Y, 0f)
        entityData.define(END_Z, 0f)
        entityData.define(LIFE, 8)
        entityData.define(HIT_BLOCK, false)
    }

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> = NetworkHooks.getEntitySpawningPacket(this)
    override fun getDamage() = damageValue
    override fun setDamage(value: Float) {
        damageValue = value.coerceAtLeast(0f)
    }

    override fun getHeadShot() = headShotMultiplier
    override fun setHeadShot(value: Float) {
        headShotMultiplier = value.coerceAtLeast(0f)
    }

    override fun getLife() = visualLife
    override fun setLife(value: Int) {
        visualLife = value.coerceIn(4, 20)
    }

    override fun isPenetrating() = true

    @Deprecated("Deprecated in Java")
    override fun isPushedByFluid() = false

    /** Called after the normal gun pipeline has applied damage, perks, position and firing direction. */
    fun prepareShot(range: Double, chargePower: Double) {
        power = chargePower.coerceIn(0.0, 1.0).toFloat()
        startTime = level().gameTime
        ownerId = owner?.id ?: -1
        val direction = deltaMovement.normalize()
        val end = position().add(direction.scale(range.coerceAtLeast(0.0)))
        val hit = level().clip(ClipContext(position(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
        hitBlock = hit.type == HitResult.Type.BLOCK
        val offset = hit.location.subtract(position())
        endX = offset.x.toFloat()
        endY = offset.y.toFloat()
        endZ = offset.z.toFloat()
        deltaMovement = Vec3.ZERO
    }

    override fun tick() {
        super.tick()
        if (level().isClientSide) return
        if (!fired) {
            // Mark before dealing damage: piercing and the visual lifetime must never repeat the shot.
            fired = true
            fireBeam()
        }
        if (level().gameTime - startTime >= visualLife) discard()
    }

    open fun canDamage(target: Entity): Boolean {
        val root = target.multipartRoot
        val shooter = owner
        return target.isAlive && root.isAlive && !root.isSpectator && target.canBeHitByProjectile()
                && root !== shooter && root !is Projectile
                && (target is PartEntity<*> || !target.isMultipartEntity)
                && !(root is Player && root.isCreative)
                && !SeekTool.IS_FRIENDLY.test(shooter, root)
                && (shooter == null || !shooter.isPassengerOfSameVehicle(root))
    }

    private fun fireBeam() {
        val start = position()
        val end = beamEnd
        val offset = end.subtract(start)
        val segments = ceil(offset.length() / 16.0).toInt().coerceAtLeast(1)
        val examined = hashSetOf<Int>()
        val hits = linkedMapOf<Int, Pair<Entity, Vec3>>()
        val source = ModDamageTypes.causeLaserDamage(level().registryAccess(), this, owner)
        val headshotSource = ModDamageTypes.causeLaserHeadshotDamage(level().registryAccess(), this, owner)
        var headshotHit = false
        // Short query boxes avoid scanning a huge diagonal bounding box for a 128-block ray.
        for (segment in 0 until segments) {
            val from = start.add(offset.scale(segment.toDouble() / segments))
            val to = start.add(offset.scale((segment + 1.0) / segments))
            for (candidate in level().getEntities(this, AABB(from, to).inflate(1.0), ::canDamage)) {
                if (!examined.add(candidate.id)) continue
                val hit = hitPosition(candidate, start, end) ?: continue
                val rootId = candidate.multipartRoot.id
                val previous = hits[rootId]
                if (previous == null || start.distanceToSqr(hit) < start.distanceToSqr(previous.second)) {
                    hits[rootId] = candidate to hit
                }
            }
        }
        // Select the nearest intersected part per parent, independent of query/part iteration order.
        for ((target, hit) in hits.values) {
            if (!canDamage(target)) continue
            val root = target.multipartRoot
            val headshot = if (target is EnderDragonPart) target === target.parentMob.head
            else root is LivingEntity && isHeadshot(hit.y - root.y, root.eyeHeight.toDouble())
            val knockback = if (root is LivingEntity) ICustomKnockback.getInstance(root) else null
            val previousStrength = knockback?.`superbWarfare$getKnockbackStrength`()
            knockback?.`superbWarfare$setKnockbackStrength`(0.0)
            try {
                root.invulnerableTime = 0
                target.invulnerableTime = 0
                val hurt = target.forceHurt(
                    if (headshot) headshotSource else source,
                    getDamage() * if (headshot) getHeadShot() else 1f
                )
                if (hurt && headshot) headshotHit = true
            } finally {
                if (previousStrength != null) knockback.`superbWarfare$setKnockbackStrength`(previousStrength)
            }
        }
        if (headshotHit) (owner as? ServerPlayer)?.sendPacket(ClientIndicatorMessage(1, 5))
    }

    private fun hitPosition(target: Entity, start: Vec3, end: Vec3): Vec3? {
        if (target is OBBEntity && !target.enableAABB()) {
            return target.getOBBs().asSequence()
                .filter { it.part != OBB.Part.COLLISION }
                .mapNotNull { it.clip(OBB.vec3ToVector3d(start), OBB.vec3ToVector3d(end)).orElse(null) }
                .map { OBB.vector3dToVec3(it) }
                .minByOrNull { start.distanceToSqr(it) }
        }
        return clipBounds(target.boundingBox, start, end)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        if (tag.contains("Damage")) setDamage(tag.getFloat("Damage"))
        if (tag.contains("HeadShot")) setHeadShot(tag.getFloat("HeadShot"))
        if (tag.contains("Life")) setLife(tag.getInt("Life"))
        if (tag.contains("Power")) power = tag.getFloat("Power").coerceIn(0f, 1f)
        if (tag.contains("StartTime")) startTime = tag.getLong("StartTime")
        if (tag.contains("Fired")) fired = tag.getBoolean("Fired")
        if (tag.contains("EndX")) endX = tag.getFloat("EndX")
        if (tag.contains("EndY")) endY = tag.getFloat("EndY")
        if (tag.contains("EndZ")) endZ = tag.getFloat("EndZ")
        if (tag.contains("HitBlock")) hitBlock = tag.getBoolean("HitBlock")
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putFloat("Damage", getDamage())
        tag.putFloat("HeadShot", getHeadShot())
        tag.putInt("Life", getLife())
        tag.putFloat("Power", power)
        tag.putLong("StartTime", startTime)
        tag.putBoolean("Fired", fired)
        tag.putFloat("EndX", endX)
        tag.putFloat("EndY", endY)
        tag.putFloat("EndZ", endZ)
        tag.putBoolean("HitBlock", hitBlock)
    }

    companion object {
        private fun radiusForPower(power: Double): Double = 0.025 + 0.155 * sqrt(power.coerceIn(0.0, 1.0))

        private fun clipBounds(bounds: AABB, start: Vec3, end: Vec3): Vec3? =
            if (bounds.contains(start)) start else bounds.clip(start, end).orElse(null)

        // Match the head region used by ProjectileEntity and IAdvancedHitDetection.
        private fun isHeadshot(relativeHitY: Double, eyeHeight: Double): Boolean =
            relativeHitY > eyeHeight - 0.25 && relativeHitY < eyeHeight + 0.3

        @JvmField
        val POWER: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val START_TIME: EntityDataAccessor<Long> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.LONG)

        @JvmField
        val OWNER_ID: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val END_X: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val END_Y: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val END_Z: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val LIFE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val HIT_BLOCK: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(LaserBeamEntity::class.java, EntityDataSerializers.BOOLEAN)
    }
}
