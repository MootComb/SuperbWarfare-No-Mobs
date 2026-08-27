package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.gun.DefaultGunResource
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.AbstractGeoItemRendererV2
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

open class GeoGunRenderer : AbstractGeoItemRendererV2() {

    override fun createAnimationInstance(stack: ItemStack, entity: Entity): IFPAnimationInstance {
        return GeoGunAnimationInstance(stack, entity, InteractionHand.MAIN_HAND)
    }

    override fun createAnimationInstance(
        stack: ItemStack,
        entity: Entity,
        hand: InteractionHand
    ): IFPAnimationInstance {
        return GeoGunAnimationInstance(stack, entity, hand)
    }

    override fun getSlotTexture(stack: ItemStack): ResourceLocation? {
        val resource = GunResource.compute(stack)
        val slotIcon = resource.slotIcon.ifEmpty { resource.icon }
        return ResourceLocation.tryParse(slotIcon)
    }

    override fun hasModel(stack: ItemStack): Boolean {
        val modelResource = GunResource.compute(stack).getModel()
        return GeoGunModel.create(modelResource) != null
    }

    override fun renderFirstPerson(
        player: LocalPlayer,
        stack: ItemStack,
        transformType: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        partialTick: Float
    ) {
        if (GunResource.compute(stack).itemDisplay[displayKey(transformType)] == null) {
            super.renderFirstPerson(player, stack, transformType, poseStack, bufferSource, packedLight, partialTick)
            return
        }

        render(stack, transformType, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, partialTick)
    }

    override fun beforeRender(
        poseStack: PoseStack,
        transformType: ItemDisplayContext,
        stack: ItemStack,
        partialTick: Float
    ) {
        val display = GunResource.compute(stack).itemDisplay[displayKey(transformType)]
        if (display != null) {
            applyItemDisplayTransform(poseStack, display)
        }
        super.beforeRender(poseStack, transformType, stack, partialTick)
    }

    override fun renderModel(
        poseStack: PoseStack,
        transformType: ItemDisplayContext,
        stack: ItemStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
        partialTick: Float
    ) {
        val resource = GunResource.compute(stack)
        val modelResource = resource.getModel()

        val useLod = transformType != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && DisplayConfig.ENABLE_GUN_LOD.get()
                && !RenderDistanceHelper.isInGui()
        val model = if (useLod) {
            GeoGunModel.create(modelResource, 1)
        } else {
            GeoGunModel.create(modelResource)
        } ?: return

        val texture = if (useLod) {
            modelResource.getLODTexture(1)
        } else {
            modelResource.texture
        } ?: return

        model.renderHand = transformType.firstPerson()
        if (transformType.firstPerson()) {
            val hand = if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                InteractionHand.OFF_HAND
            } else {
                InteractionHand.MAIN_HAND
            }
            val pose = FirstPersonRenderHandler.getActiveAnimationInstance(hand)?.cachedPose
            if (pose != null) {
                model.applyPose(BLENDER.blend(model.getBindPose(), pose))
            }

            val sprintOffset = resource.sprintOffset
            ClientEventHandler.gunRootMoveV2(poseStack, sprintOffset.x, sprintOffset.y, sprintOffset.z, false)
            applyFirstPersonPositioningTransform(poseStack, model)
        }
        model.renderToBuffer(poseStack, bufferSource, texture, packedLight, packedOverlay)
        model.resetPose()
    }

    private fun applyFirstPersonPositioningTransform(poseStack: PoseStack, model: GeoGunModel) {
        val idleViewTransform = model.getGlobalTransform(IDLE_VIEW_BONE) ?: return
        val zoom = AnimationCurves.EASE_IN_OUT_QUINT
            .apply(ClientEventHandler.zoomTime.coerceIn(0.0, 1.0))
            .toFloat()

        val viewTransform = if (zoom <= 0f) {
            Matrix4f(idleViewTransform)
        } else {
            val ironViewTransform = model.getGlobalTransform(IRON_VIEW_BONE)
            if (ironViewTransform == null) {
                Matrix4f(idleViewTransform)
            } else {
                blendViewTransform(Matrix4f(idleViewTransform), Matrix4f(ironViewTransform), zoom)
            }
        }

        poseStack.mulPose(viewTransform.invert())
    }

    private fun blendViewTransform(from: Matrix4f, to: Matrix4f, t: Float): Matrix4f {
        val translation = Vector3f()
        val toTranslation = Vector3f()
        from.getTranslation(translation)
        to.getTranslation(toTranslation)
        translation.lerp(toTranslation, t)

        val rotation = Quaternionf()
        val toRotation = Quaternionf()
        from.getNormalizedRotation(rotation)
        to.getNormalizedRotation(toRotation)
        rotation.slerp(toRotation, t)

        val scale = Vector3f()
        val toScale = Vector3f()
        from.getScale(scale)
        to.getScale(toScale)
        scale.lerp(toScale, t)

        return Matrix4f()
            .translation(translation)
            .rotate(rotation)
            .scale(scale)
    }

    private fun applyItemDisplayTransform(poseStack: PoseStack, display: DefaultGunResource.ItemDisplayInfo) {
        val translation = display.translation
        poseStack.translate(translation[0] / 16f, translation[1] / 16f, translation[2] / 16f)

        val rotation = display.rotation
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation[0]))
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation[1]))
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation[2]))

        val scale = display.scale
        poseStack.scale(scale[0], scale[1], scale[2])
    }

    private fun displayKey(transformType: ItemDisplayContext): String {
        return when (transformType) {
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND -> "firstperson_righthand"
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND -> "firstperson_lefthand"
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND -> "thirdperson_righthand"
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND -> "thirdperson_lefthand"
            ItemDisplayContext.GUI -> "gui"
            ItemDisplayContext.GROUND -> "ground"
            ItemDisplayContext.HEAD -> "head"
            ItemDisplayContext.FIXED -> "fixed"
            else -> ""
        }
    }

    companion object {
        private const val IDLE_VIEW_BONE = "idle_view"
        private const val IRON_VIEW_BONE = "iron_view"

        private val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
