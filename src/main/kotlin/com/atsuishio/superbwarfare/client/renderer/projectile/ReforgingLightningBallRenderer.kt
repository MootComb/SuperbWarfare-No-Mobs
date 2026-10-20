package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.entity.projectile.ReforgingLightningBallEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import java.util.*

class ReforgingLightningBallRenderer(context: EntityRendererProvider.Context) :
    EntityRenderer<ReforgingLightningBallEntity>(context) {

    private val arcRandom = Random()

    override fun render(
        entity: ReforgingLightningBallEntity,
        entityYaw: Float,
        partialTicks: Float,
        pose: PoseStack,
        buffers: MultiBufferSource,
        packedLight: Int,
    ) {
        pose.pushPose()

        // 与普通子弹一致：弹体从眼位生成，开火头几 tick 挪到枪口再淡出
        // （见 [ClientRenderHandler.bulletRenderOffset]）。电弧的端点都是相对实体位置算的，
        // 跟着同一份 pose 走才不会和球体分家。
        ClientRenderHandler.transformVirtualRenderPosition(pose, entity, partialTicks)

        val buffer = buffers.getBuffer(RenderType.lightning())
        val phase = entity.level().gameTime / 2
        val seed = entity.id.toLong() * 31 + phase * 17
        val model = entity.getModelInstance()
        val animation = entity.getAnimationInstance()
        if (model != null && animation != null) {
            animation.context.partialTick = partialTicks
            animation.tick()
            model.applyPose(BasicProjectileRenderer.BLENDER.blend(model.bindPose, animation.getPose()))
            model.renderToBuffer(
                pose, buffers.getBuffer(RenderType.eyes(TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
            )
            // Both outer cages carry electricity along all edges and across their rotating faces.
            for ((boneName, halfSize) in ELECTRIC_CAGES) {
                val bone = model.getIndex(boneName)
                if (bone < 0) continue
                val transform = Matrix4f(pose.last().pose()).mul(model.getGlobalTransform(bone))
                electrifyCage(buffer, transform, halfSize, seed + bone * 101L, phase)
            }
        }

        for (arc in entity.lightningArcs) {
            val end = arc.end.subtract(entity.getPosition(partialTicks))
            lightning(buffer, pose.last().pose(), Vec3.ZERO, end, arc.seed, (arc.ticks - partialTicks) / 4f)
        }
        pose.popPose()
        super.render(entity, entityYaw, partialTicks, pose, buffers, packedLight)
    }

    private fun electrifyCage(
        buffer: VertexConsumer, pose: Matrix4f, halfSize: Double, seed: Long, phase: Long,
    ) {
        val corners = CAGE_CORNERS.map { it.scale(halfSize) }
        for (index in CAGE_EDGES.indices) {
            val edge = CAGE_EDGES[index]
            lightning(buffer, pose, corners[edge.first], corners[edge.second], seed + index * 37L, 0.65f, 3)
        }
        repeat(3) { faceIndex ->
            val face = Math.floorMod(phase + faceIndex * 2, 6)
            val axis = face / 2
            val start = if (face % 2 == 0) 0 else 1 shl axis
            val end = start xor (7 xor (1 shl axis))
            lightning(buffer, pose, corners[start], corners[end], seed + 1000 + faceIndex, 0.85f, 5)
        }
        // A fourth arc bridges the charged shell and its core.
        lightning(buffer, pose, Vec3.ZERO, corners[(phase.toInt() and 7)], seed + 2000, 0.5f, 4)
    }

    private fun lightning(
        buffer: VertexConsumer, pose: Matrix4f, start: Vec3, end: Vec3, seed: Long, alpha: Float,
        maxSegments: Int = 32,
    ) {
        val offset = end.subtract(start)
        val length = offset.length()
        if (length < 0.01 || alpha <= 0f) return
        val random = arcRandom
        random.setSeed(seed)
        val segments = (length * 3).toInt().coerceIn(3, maxSegments)
        val jitter = (length * 0.06).coerceIn(0.08, 0.35)
        var previous = start
        for (i in 1..segments) {
            val point = if (i == segments) end else start.add(offset.scale(i.toDouble() / segments)).add(
                (random.nextDouble() - 0.5) * jitter * 2,
                (random.nextDouble() - 0.5) * jitter * 2,
                (random.nextDouble() - 0.5) * jitter * 2,
            )
            beam(buffer, pose, previous, point, 0.065, 0.2f, 0.4f, 1f, alpha * 0.25f)
            beam(buffer, pose, previous, point, 0.018, 0.8f, 0.9f, 1f, alpha)
            previous = point
        }
    }

    /** Four faces around a segment give the frame edges actual thickness from every view. */
    private fun beam(
        buffer: VertexConsumer, pose: Matrix4f, start: Vec3, end: Vec3,
        width: Double, red: Float, green: Float, blue: Float, alpha: Float,
    ) {
        val direction = end.subtract(start).normalize()
        val reference = if (kotlin.math.abs(direction.y) > 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val side = direction.cross(reference).normalize().scale(width)
        val up = direction.cross(side).normalize().scale(width)
        val offsets = arrayOf(side.add(up), side.subtract(up), side.scale(-1.0).subtract(up), up.subtract(side))
        for (i in offsets.indices) {
            val next = (i + 1) % offsets.size
            for (point in arrayOf(
                start.add(offsets[i]),
                end.add(offsets[i]),
                end.add(offsets[next]),
                start.add(offsets[next])
            )) {
                buffer.vertex(pose, point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                    .color(red, green, blue, alpha).endVertex()
            }
        }
    }

    override fun getTextureLocation(entity: ReforgingLightningBallEntity) = TEXTURE

    companion object {
        private val TEXTURE = loc("textures/bedrock/projectile/reforging_lightning_ball.png")
        private val ELECTRIC_CAGES = arrayOf("middle_frame" to 13.4 / 16.0, "outer_frame" to 16.8 / 16.0)
        private val CAGE_CORNERS = Array(8) { corner ->
            Vec3(
                if (corner and 1 == 0) -1.0 else 1.0,
                if (corner and 2 == 0) -1.0 else 1.0,
                if (corner and 4 == 0) -1.0 else 1.0
            )
        }
        private val CAGE_EDGES = buildList {
            for (axis in 0..2) {
                val bit = 1 shl axis
                for (corner in 0..7) if (corner and bit == 0) add(corner to (corner xor bit))
            }
        }
    }
}
