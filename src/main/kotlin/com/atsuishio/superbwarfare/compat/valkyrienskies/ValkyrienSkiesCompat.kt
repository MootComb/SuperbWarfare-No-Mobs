package com.atsuishio.superbwarfare.compat.valkyrienskies

import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.compat.valkyrienskies.ValkyrienSkiesCompat.toWorldDirection
import com.atsuishio.superbwarfare.entity.projectile.IAdvancedHitDetection
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.fml.ModList
import org.joml.Matrix4d
import org.joml.Matrix4dc
import org.joml.Vector3d
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.mod.api.getShipsIntersecting
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.api.toMinecraft
import org.valkyrienskies.mod.common.getLevelFromDimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils
import java.util.function.Predicate
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ValkyrienSkiesCompat {

    @JvmStatic
    fun hasMod(): Boolean {
        return ModList.get().isLoaded(CompatHolder.VALKYRIEN_SKIES)
    }

    @JvmStatic
    fun adjustMovementForShipCollisions(
        entity: Entity,
        movement: Vec3,
        boundingBox: AABB,
        level: Level
    ): Vec3 {
        return try {
            EntityShipCollisionUtils.adjustEntityMovementForShipCollisions(
                entity, movement, boundingBox, level
            )
        } catch (_: Exception) {
            movement
        }
    }

    /**
     * 将 VS 船舶上的局部坐标转换为世界绝对坐标。
     * 通过区块归属查询管理该位置的船舶，不受船舶当前世界AABB限制。
     * 若该位置未在任何船舶上，则原样返回。
     *
     * @param level 当前世界
     * @param pos   待转换的坐标（船舶局部或世界坐标）
     * @return 世界绝对坐标
     */
    @JvmStatic
    fun toWorldSpace(level: Level, pos: Vec3): Vec3 {
        if (level !is ServerLevel) return pos

        return try {
            val blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
            val ship = level.getLoadedShipManagingPos(blockPos) ?: return pos
            val jomlPos = Vector3d(pos.toJOML())
            (ship as Ship).shipToWorld.transformPosition(jomlPos)
            Vec3(jomlPos.x, jomlPos.y, jomlPos.z)
        } catch (_: Exception) {
            pos
        }
    }

    /**
     * 将实体的位置转换为世界空间坐标。
     * 通过实体所在区块查找管理船舶，可在服务端和客户端使用。
     * 若实体未在 VS 船舶上，则返回其原始 position()。
     *
     * @param entity 待转换位置的实体
     * @return 世界空间坐标
     */
    @JvmStatic
    fun toWorldSpace(entity: Entity): Vec3 {
        return try {
            val level = entity.level()
            val chunkX = entity.blockX shr 4
            val chunkZ = entity.blockZ shr 4
            val ship = level.getLoadedShipManagingPos(chunkX, chunkZ) ?: return entity.position()
            val jomlPos = Vector3d(entity.position().toJOML())
            (ship as Ship).shipToWorld.transformPosition(jomlPos)
            Vec3(jomlPos.x, jomlPos.y, jomlPos.z)
        } catch (_: Exception) {
            entity.position()
        }
    }

    /**
     * 将实体附近的某个坐标转换为世界空间坐标。
     * 通过实体所在区块查找管理船舶，适用于需要转换插值坐标的渲染场景。
     * 若实体未在 VS 船舶上，则原样返回输入坐标。
     *
     * @param pos    待转换的坐标（通常为实体的插值坐标）
     * @param entity 用于确定所属船舶的实体
     * @return 世界空间坐标
     */
    @JvmStatic
    fun toWorldPos(pos: Vec3, entity: Entity): Vec3 {
        return try {
            val level = entity.level()
            val chunkX = entity.blockX shr 4
            val chunkZ = entity.blockZ shr 4
            val ship = level.getLoadedShipManagingPos(chunkX, chunkZ) ?: return pos
            val jomlPos = Vector3d(pos.toJOML())
            (ship as Ship).shipToWorld.transformPosition(jomlPos)
            Vec3(jomlPos.x, jomlPos.y, jomlPos.z)
        } catch (_: Exception) {
            pos
        }
    }

    /**
     * 将实体局部方向向量转换为世界空间方向。
     * 通过实体所在区块查找管理船舶，可在服务端和客户端使用。
     * 若实体未在 VS 船舶上，则原样返回输入方向。
     *
     * @param entity   用于确定所属船舶的实体
     * @param localDir 实体局部方向向量
     * @return 世界空间方向向量
     */
    @JvmStatic
    fun toWorldDirection(entity: Entity, localDir: Vec3): Vec3 {
        return try {
            val level = entity.level()
            val chunkX = entity.blockX shr 4
            val chunkZ = entity.blockZ shr 4
            val ship = level.getLoadedShipManagingPos(chunkX, chunkZ) ?: return localDir
            val jomlDir = Vector3d(localDir.toJOML())
            (ship as Ship).shipToWorld.transformDirection(jomlDir)
            Vec3(jomlDir.x, jomlDir.y, jomlDir.z)
        } catch (_: Exception) {
            localDir
        }
    }

    /**
     * 将世界坐标转换为 VS 船舶上的局部坐标。
     * 通过区块归属查询管理该位置的船舶，不受船舶当前世界AABB限制。
     * 若该位置未在任何船舶上，则原样返回。
     *
     * @param level 当前世界
     * @param pos   待转换的世界坐标
     * @return 船舶局部坐标（若不在船上则返回原值）
     */
    @JvmStatic
    fun toShipSpace(level: Level, pos: Vec3): Vec3 {
        if (level !is ServerLevel) return pos

        return try {
            val blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
            val ship = level.getLoadedShipManagingPos(blockPos) ?: return pos
            val jomlPos = Vector3d(pos.toJOML())
            (ship as Ship).worldToShip.transformPosition(jomlPos)
            Vec3(jomlPos.x, jomlPos.y, jomlPos.z)
        } catch (_: Exception) {
            pos
        }
    }

    /**
     * 获取 VS 船舶在指定位置的世界空间 Y 轴旋转角（度），用于雷达等需要与船舶朝向同步的功能。
     * 若该位置未在任何船舶上，返回 null。
     *
     * @param level 当前世界
     * @param pos   待查询的坐标
     * @return 船舶的 Y 轴旋转角（yaw），未找到船舶时返回 null
     */
    @JvmStatic
    fun getShipYaw(level: Level, pos: Vec3): Double? {
        if (level !is ServerLevel) return null

        return try {
            val blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
            val ship = level.getLoadedShipManagingPos(blockPos) ?: return null
            val forward = Vector3d(0.0, 0.0, -1.0)
            (ship as Ship).shipToWorld.transformDirection(forward)
            Math.toDegrees(atan2(-forward.x, forward.z)) + 180.0
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    fun getShipYaw(entity: Entity): Float? {
        return try {
            val level = entity.level()
            val chunkX = entity.blockX shr 4
            val chunkZ = entity.blockZ shr 4
            val ship = level.getLoadedShipManagingPos(chunkX, chunkZ)
            if (ship != null) {
                val forward = Vector3d(0.0, 0.0, 1.0)
                (ship as Ship).shipToWorld.transformDirection(forward)
                return Math.toDegrees(atan2(-forward.x, forward.z)).toFloat()
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    fun getShipPitch(entity: Entity): Float? {
        return try {
            val level = entity.level()
            val chunkX = entity.blockX shr 4
            val chunkZ = entity.blockZ shr 4
            val ship = level.getLoadedShipManagingPos(chunkX, chunkZ) ?: return null
            val forward = Vector3d(0.0, 0.0, 1.0)
            (ship as Ship).shipToWorld.transformDirection(forward)
            val hLen = sqrt(forward.x * forward.x + forward.z * forward.z)
            Math.toDegrees(atan2(-forward.y, hLen)).toFloat()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取实体所在 VS 船舶的世界空间 Z 轴旋转角/横滚角（度）。
     * 通过船舶的 up 向量在 XY 平面的投影计算。
     * 仅对固定在物理体上的载具生效。
     */
    @JvmStatic
    fun getShipRoll(entity: Entity): Float? {
        return try {
            val level = entity.level()
            val chunkX = entity.blockX shr 4
            val chunkZ = entity.blockZ shr 4
            val ship = level.getLoadedShipManagingPos(chunkX, chunkZ) ?: return null
            val up = Vector3d(0.0, 1.0, 0.0)
            (ship as Ship).shipToWorld.transformDirection(up)
            Math.toDegrees(atan2(up.x, up.y)).toFloat()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 将 VS 船舶上的局部方向向量转换为世界空间方向。
     * 用于弹射器、传送带等需要跟随船舶旋转的方块。
     * 若该位置未在任何船舶上，则原样返回。
     *
     * @param level    当前世界
     * @param pos      待查询的坐标
     * @param localDir 船舶局部方向向量
     * @return 世界空间方向向量
     */
    @JvmStatic
    fun toWorldDirection(level: Level, pos: Vec3, localDir: Vec3): Vec3 {
        if (!hasMod()) return localDir

        return try {
            val blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
            val ship = level.getLoadedShipManagingPos(blockPos) ?: return localDir
            val jomlDir = Vector3d(localDir.toJOML())
            (ship as Ship).shipToWorld.transformDirection(jomlDir)
            Vec3(jomlDir.x, jomlDir.y, jomlDir.z)
        } catch (_: Exception) {
            localDir
        }
    }

    /**
     * 将世界空间方向向量转换为 VS 船舶局部方向。
     * 与 [toWorldDirection] 互为逆操作。
     *
     * @param level    当前世界
     * @param pos      待查询的坐标
     * @param worldDir 世界空间方向向量
     * @return 船舶局部方向向量
     */
    @JvmStatic
    fun toShipDirection(level: Level, pos: Vec3, worldDir: Vec3): Vec3 {
        return try {
            val blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
            // 优先区块归属查询
            val ship = level.getLoadedShipManagingPos(blockPos)
            if (ship != null) {
                val jomlDir = Vector3d(worldDir.toJOML())
                (ship as Ship).worldToShip.transformDirection(jomlDir)
                return Vec3(jomlDir.x, jomlDir.y, jomlDir.z)
            }

            // 回退：世界空间 AABB 查询
            val queryAabb = AABBd(
                pos.x - 2.0, pos.y - 2.0, pos.z - 2.0,
                pos.x + 2.0, pos.y + 2.0, pos.z + 2.0
            ).correctBounds()
            val ships = level.getShipsIntersecting(queryAabb)
            for (s in ships) {
                val jomlDir = Vector3d(worldDir.toJOML())
                s.worldToShip.transformDirection(jomlDir)
                return Vec3(jomlDir.x, jomlDir.y, jomlDir.z)
            }

            worldDir
        } catch (_: Exception) {
            worldDir
        }
    }

    class ShipTransformCache(private val entries: List<Triple<Matrix4dc, AABBdc, Vector3d>>) {
        /** (worldToShip: Matrix4dc, worldAABB: AABBdc, tmpVec: Vector3d) */
        val isEmpty: Boolean get() = entries.isEmpty()

        companion object {
            @JvmStatic
            fun create(level: Level, explosionAABB: AABB): ShipTransformCache {
                if (level !is ServerLevel) return ShipTransformCache(emptyList())

                return try {
                    val minVec = Vec3(explosionAABB.minX, explosionAABB.minY, explosionAABB.minZ).toJOML()
                    val maxVec = Vec3(explosionAABB.maxX, explosionAABB.maxY, explosionAABB.maxZ).toJOML()
                    val queryAabb = AABBd(minVec, maxVec).correctBounds()

                    val ships = level.getShipsIntersecting(queryAabb)

                    val entries = mutableListOf<Triple<Matrix4dc, AABBdc, Vector3d>>()
                    for (ship in ships) {
                        val wts = ship.worldToShip
                        val aabb = ship.worldAABB
                        entries.add(Triple(wts, aabb, Vector3d()))
                    }
                    ShipTransformCache(entries)
                } catch (_: Exception) {
                    ShipTransformCache(emptyList())
                }
            }
        }

        fun toShipSpace(worldPos: BlockPos): BlockPos? {
            if (isEmpty) return null

            return try {
                val jomlWorld = Vec3.atCenterOf(worldPos).toJOML()
                for ((worldToShip, worldAABB, _) in entries) {
                    val contained = worldAABB.containsPoint(jomlWorld)
                    if (!contained) continue

                    worldToShip.transformPosition(jomlWorld)
                    return BlockPos.containing(jomlWorld.toMinecraft())
                }
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * 对瓦尔基里天空模组的船体方块进行射线检测。
     * 将射线从世界坐标转换到船舶空间，在船舶维度中执行与原版世界相同的方块射线追踪（DDA 遍历），
     * 然后将命中结果转换回世界坐标。
     *
     * @param level 当前世界（必须是 ServerLevel）
     * @param startVec 射线起点（世界坐标）
     * @param endVec 射线终点（世界坐标）
     * @param ignorePredicate 忽略特定方块的谓词（在船舶空间对方块状态进行测试）
     * @return 命中位置和方块坐标（世界空间），如果未命中则返回 null
     */
    @JvmStatic
    fun rayTraceShipBlocks(
        level: Level,
        startVec: Vec3,
        endVec: Vec3,
        ignorePredicate: Predicate<BlockState>
    ): Pair<Vec3, BlockPos>? {
        if (level !is ServerLevel) return null

        return try {
            // 构建射线包围盒，用于查询相交的船舶
            val minVec = Vec3(
                min(startVec.x, endVec.x) - 1.0,
                min(startVec.y, endVec.y) - 1.0,
                min(startVec.z, endVec.z) - 1.0
            ).toJOML()
            val maxVec = Vec3(
                max(startVec.x, endVec.x) + 1.0,
                max(startVec.y, endVec.y) + 1.0,
                max(startVec.z, endVec.z) + 1.0
            ).toJOML()
            val queryAabb = AABBd(minVec, maxVec).correctBounds()
            val ships = level.getShipsIntersecting(queryAabb)

            val server = level.server
            var closestDistSqr = Double.MAX_VALUE
            var closestHit: Pair<Vec3, BlockPos>? = null

            for (ship in ships) {
                val shipHit = rayTraceSingleShip(ship, server, startVec, endVec, ignorePredicate)
                    ?: continue
                val (hitPos, _) = shipHit
                val distSqr = startVec.distanceToSqr(hitPos)
                if (distSqr < closestDistSqr) {
                    closestDistSqr = distSqr
                    closestHit = shipHit
                }
            }

            closestHit
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 对单个船舶进行射线检测。
     * 将射线转换到船舶空间，在船舶维度中使用与原版世界相同的 DDA 方块遍历进行射线追踪，
     * 然后将命中转换回世界空间。
     *
     * 使用 [com.atsuishio.superbwarfare.entity.projectile.IAdvancedHitDetection.Companion.performRayTrace]
     * 在船舶维度上进行射线追踪，以避免 VS 对 Level.clip() 的 mixin 拦截产生递归。
     */
    private fun rayTraceSingleShip(
        ship: Ship,
        server: net.minecraft.server.MinecraftServer,
        startVec: Vec3,
        endVec: Vec3,
        ignorePredicate: Predicate<BlockState>
    ): Pair<Vec3, BlockPos>? {
        return try {
            val worldToShip = ship.worldToShip

            // 将射线转换到船舶空间
            val shipStart = startVec.toJOML().also { worldToShip.transformPosition(it) }.toMinecraft()
            val shipEnd = endVec.toJOML().also { worldToShip.transformPosition(it) }.toMinecraft()

            // 获取船舶所在维度的 ServerLevel
            val shipLevel = server.getLevelFromDimensionId(ship.chunkClaimDimension) ?: return null

            // 在船舶维度中进行方块射线追踪（使用与原版世界相同的 DDA 遍历逻辑）
            val shipContext = ClipContext(
                shipStart, shipEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            )
            val shipResult = IAdvancedHitDetection.performRayTrace(
                shipContext,
                { rayTraceContext, blockPos ->
                    val blockState: BlockState = shipLevel.getBlockState(blockPos)
                    if (ignorePredicate.test(blockState)) return@performRayTrace null
                    val fluidState = shipLevel.getFluidState(blockPos)
                    val blockShape = rayTraceContext.getBlockShape(blockState, shipLevel, blockPos)
                    val blockResult = shipLevel.clipWithInteractionOverride(
                        rayTraceContext.from, rayTraceContext.to, blockPos, blockShape, blockState
                    )
                    val fluidShape = rayTraceContext.getFluidShape(fluidState, shipLevel, blockPos)
                    val fluidResult = fluidShape.clip(rayTraceContext.from, rayTraceContext.to, blockPos)
                    val blockDistance = blockResult?.let { rayTraceContext.from.distanceToSqr(it.location) }
                        ?: Double.MAX_VALUE
                    val fluidDistance = fluidResult?.let { rayTraceContext.from.distanceToSqr(it.location) }
                        ?: Double.MAX_VALUE
                    if (blockDistance <= fluidDistance) blockResult else fluidResult
                },
                { rayTraceContext ->
                    val vec3 = rayTraceContext.from.subtract(rayTraceContext.to)
                    BlockHitResult.miss(
                        rayTraceContext.to,
                        net.minecraft.core.Direction.getNearest(vec3.x, vec3.y, vec3.z),
                        BlockPos.containing(rayTraceContext.to)
                    )
                }
            )

            if (shipResult.type == HitResult.Type.MISS) return null

            // 将命中位置转换回世界空间
            // 使用 worldToShip 的逆矩阵而不是 shipToWorld，确保使用完全一致的坐标系统
            val worldPos = worldToShip
                .invert(Matrix4d())
                .transformPosition(shipResult.location.toJOML())
            val worldHitPos = Vec3(worldPos.x, worldPos.y, worldPos.z)
            val worldBlockPos = BlockPos.containing(worldHitPos)

            Pair(worldHitPos, worldBlockPos)
        } catch (_: Exception) {
            null
        }
    }
}
