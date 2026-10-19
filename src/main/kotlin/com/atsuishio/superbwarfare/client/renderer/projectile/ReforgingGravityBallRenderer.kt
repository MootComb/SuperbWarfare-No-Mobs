package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.ReforgingGravityBallEntity
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ReforgingGravityBallRenderer(context: EntityRendererProvider.Context) :
    EntityRenderer<ReforgingGravityBallEntity>(context) {

    override fun render(
        entity: ReforgingGravityBallEntity, entityYaw: Float, partialTicks: Float,
        pose: PoseStack, buffers: MultiBufferSource, packedLight: Int,
    ) {
        val expansion = entity.expansionProgress(partialTicks)
        val model = entity.getModelInstance()
        val animation = entity.getAnimationInstance()
        if (model != null && animation != null) {
            animation.context.partialTick = partialTicks
            animation.tick()
            model.applyPose(BasicProjectileRenderer.BLENDER.blend(model.bindPose, animation.getPose()))
            val frame = model.getBone("frame")
            val singularity = model.getBone("move_singularity")
            val glow = buffers.getBuffer(RenderType.eyes(TEXTURE))

            if (entity.anchored && expansion > 0f) {
                frame?.visible = true
                singularity?.visible = false
                val scale = 0.03f + 0.97f * expansion
                pose.pushPose()
                pose.scale(scale, scale * (0.25f + 0.75f * expansion), scale)
                model.renderToBuffer(pose, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                pose.popPose()
            }

            // A solid, roughly 0.1-block cube is the entire projectile while it is airborne.
            if (expansion < 1f) {
                frame?.visible = false
                singularity?.visible = true
                val scale = if (entity.collapsing) 0.25f else 0.25f + 0.5f * expansion
                pose.pushPose()
                pose.scale(scale, scale, scale)
                model.renderToBuffer(
                    pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1f, 1f, 1f, 1f - expansion
                )
                pose.popPose()
            }
        }

        if (entity.anchored && expansion > 0f && expansion < 1f) {
            val buffer = buffers.getBuffer(RenderType.lightning())
            val radius = 0.1 + expansion * (if (entity.collapsing) entity.coreRadius else 1.5)
            val fade = sin(PI * expansion).toFloat() * 0.65f
            for (segment in 0 until 40) {
                val angle = segment * PI / 20
                val next = (segment + 1) * PI / 20
                ribbon(
                    buffer, pose.last().pose(),
                    Vec3(cos(angle) * radius, 0.0, sin(angle) * radius),
                    Vec3(cos(next) * radius, 0.0, sin(next) * radius),
                    0.025, fade
                )
            }
        }

        if (entity.fieldActive) {
            val time = entity.level().gameTime - entity.activationTime + partialTicks.toDouble()
            val buffer = buffers.getBuffer(RenderType.lightning())
            for (stream in 0 until 8) {
                // The leading end advances toward the center while the tail remains farther out.
                val head = (time / 36.0 + stream / 8.0) % 1.0
                var previous: Vec3? = null
                for (segment in 0..12) {
                    val progress = (head - 0.18 + segment * 0.015).coerceAtLeast(0.0)
                    val radius = entity.attackRadius * (1 - progress)
                    val angle = stream * PI / 4 + progress * PI * 3 + time * 0.025
                    val point = Vec3(cos(angle) * radius, sin(angle * 0.7 + stream) * radius * 0.45, sin(angle) * radius)
                    val start = previous
                    if (start != null && start.distanceToSqr(point) > 0.00001) {
                        val fade = (sin(PI * progress) * segment / 12.0).toFloat()
                        ribbon(buffer, pose.last().pose(), start, point, 0.09, fade * 0.14f)
                        ribbon(buffer, pose.last().pose(), start, point, 0.018, fade * 0.75f)
                    }
                    previous = point
                }
            }
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, packedLight)
    }

    private fun ribbon(buffer: VertexConsumer, pose: Matrix4f, start: Vec3, end: Vec3, width: Double, alpha: Float) {
        val direction = end.subtract(start).normalize()
        val reference = if (kotlin.math.abs(direction.y) > 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
        val side = direction.cross(reference).normalize().scale(width)
        val up = direction.cross(side).normalize().scale(width)
        for (offset in arrayOf(side, up)) {
            for (point in arrayOf(start.add(offset), end.add(offset), end.subtract(offset), start.subtract(offset))) {
                buffer.vertex(pose, point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                    .color(1f, 0.78f, 0.15f, alpha).endVertex()
            }
        }
    }

    override fun getTextureLocation(entity: ReforgingGravityBallEntity) = TEXTURE

    companion object {
        private val TEXTURE = loc("textures/bedrock/projectile/reforging_gravity_ball.png")
    }
}
