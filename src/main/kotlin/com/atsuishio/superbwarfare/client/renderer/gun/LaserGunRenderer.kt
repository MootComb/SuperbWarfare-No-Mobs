package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.projectile.LaserBeamRenderer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

/** Opt-in gun renderer for weapons whose projectiles originate at an animated muzzle locator. */
open class LaserGunRenderer : GeoGunRenderer() {
    override fun renderAttachments(
        stack: ItemStack, model: GeoGunModel, transformType: ItemDisplayContext,
        poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int,
    ) {
        super.renderAttachments(stack, model, transformType, poseStack, bufferSource, packedLight, packedOverlay)
        LaserBeamRenderer.captureMuzzle(poseStack, model, transformType)
    }
}
