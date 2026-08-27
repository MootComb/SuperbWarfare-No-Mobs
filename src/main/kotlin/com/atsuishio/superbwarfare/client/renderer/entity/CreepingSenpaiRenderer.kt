package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.CreepingSenpaiEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation

class CreepingSenpaiRenderer(renderManager: EntityRendererProvider.Context) :
    EntityRenderer<CreepingSenpaiEntity>(renderManager) {
    init {
        this.shadowRadius = 0.5f
    }

    override fun getTextureLocation(pEntity: CreepingSenpaiEntity): ResourceLocation {
        return TEXTURE
    }

    override fun render(
        pEntity: CreepingSenpaiEntity,
        pEntityYaw: Float,
        pPartialTick: Float,
        pPoseStack: PoseStack,
        pBuffer: MultiBufferSource,
        pPackedLight: Int
    ) {
        val ani = pEntity.animationInstance ?: return
        val instance = pEntity.modelInstance ?: return

        val progress = pEntity.renderFaceProgress(pPartialTick)
        val eased = progress * progress * (3f - 2f * progress)
        val startAngles = faceAngles(pEntity.renderStartFace)
        val targetAngles = faceAngles(pEntity.renderTargetFace)
        val startOffset = faceOffset(pEntity.renderStartFace, pEntity)
        val targetOffset = faceOffset(pEntity.renderTargetFace, pEntity)
        val offsetX = startOffset.first + (targetOffset.first - startOffset.first) * eased
        val offsetY = startOffset.second + (targetOffset.second - startOffset.second) * eased
        val offsetZ = startOffset.third + (targetOffset.third - startOffset.third) * eased
        val pitch = startAngles.first + (targetAngles.first - startAngles.first) * eased
        val roll = startAngles.second + (targetAngles.second - startAngles.second) * eased

        pPoseStack.pushPose()
        pPoseStack.translate(offsetX.toDouble(), offsetY.toDouble(), offsetZ.toDouble())
        pPoseStack.mulPose(Axis.YP.rotationDegrees(180f))
        pPoseStack.mulPose(Axis.YP.rotationDegrees(-pEntity.getViewYRot(pPartialTick)))
        pPoseStack.mulPose(Axis.XP.rotationDegrees(pitch))
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(roll))

        ani.context.partialTick = pPartialTick
        ani.tick()
        instance.applyPose(BLENDER.blend(instance.bindPose, ani.getPose()))

        instance.renderToBuffer(
            pPoseStack,
            pBuffer,
            RenderType.entityCutout(getTextureLocation(pEntity)),
            BedrockModelRenderTypes.polyMeshCutout(getTextureLocation(pEntity)),
            pPackedLight,
            OverlayTexture.pack(0f, pEntity.hurtTime > 0 || pEntity.deathTime > 0)
        )
        pPoseStack.popPose()
    }

    private fun faceAngles(face: Direction): Pair<Float, Float> {
        return when (face) {
            Direction.DOWN -> 0f to 180f
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST -> 90f to 0f
            Direction.UP -> 0f to 0f
        }
    }

    private fun faceOffset(face: Direction, entity: CreepingSenpaiEntity): Triple<Float, Float, Float> {
        return when (face) {
            Direction.DOWN -> Triple(0f, entity.bbHeight, 0f)
            Direction.NORTH -> Triple(0f, 0f, entity.bbWidth / 2f)
            Direction.SOUTH -> Triple(0f, 0f, -entity.bbWidth / 2f)
            Direction.EAST -> Triple(-entity.bbWidth / 2f, 0f, 0f)
            Direction.WEST -> Triple(entity.bbWidth / 2f, 0f, 0f)
            Direction.UP -> Triple(0f, 0f, 0f)
        }
    }

    companion object {
        var TEXTURE = loc("textures/bedrock/entity/creeping_senpai.png")
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
