package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
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

    override fun render(
        entity: ReforgingLightningBallEntity,
        entityYaw: Float,
        partialTicks: Float,
        pose: PoseStack,
        buffers: MultiBufferSource,
        packedLight: Int,
    ) {
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
        }

        val buffer = buffers.getBuffer(RenderType.lightning())
        for (arc in entity.lightningArcs) {
            val end = arc.end.subtract(entity.getPosition(partialTicks))
            lightning(buffer, pose.last().pose(), end, arc.seed, (arc.ticks - partialTicks) / 4f)
        }

        // Small arcs between the cages keep an idle ball visibly electrified.
        val random = Random(entity.id.toLong() * 31 + entity.level().gameTime / 3)
        repeat(3) {
            val end = Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5)
                .normalize().scale(1.25)
            lightning(buffer, pose.last().pose(), end, random.nextLong(), 0.45f)
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, packedLight)
    }

    private fun lightning(buffer: VertexConsumer, pose: Matrix4f, end: Vec3, seed: Long, alpha: Float) {
        if (end.lengthSqr() < 0.0001 || alpha <= 0f) return
        val random = Random(seed)
        val segments = (end.length() * 3).toInt().coerceIn(5, 32)
        val jitter = (end.length() * 0.06).coerceIn(0.08, 0.35)
        var previous = Vec3.ZERO
        for (i in 1..segments) {
            val point = if (i == segments) end else end.scale(i.toDouble() / segments).add(
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
            for (point in arrayOf(start.add(offsets[i]), end.add(offsets[i]), end.add(offsets[next]), start.add(offsets[next]))) {
                buffer.vertex(pose, point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                    .color(red, green, blue, alpha).endVertex()
            }
        }
    }

    override fun getTextureLocation(entity: ReforgingLightningBallEntity) = TEXTURE

    companion object {
        private val TEXTURE = loc("textures/bedrock/projectile/reforging_lightning_ball.png")
    }
}
