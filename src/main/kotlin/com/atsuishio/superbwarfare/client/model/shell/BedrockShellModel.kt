package com.atsuishio.superbwarfare.client.model.shell

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType

class BedrockShellModel(private val baseModel: TreeBedrockModel) {
    private val instance: TreeModelInstance = baseModel.createInstance()

    fun renderToBuffer(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        quadRenderType: RenderType,
        triangleRenderType: RenderType,
        packedLight: Int,
        packedOverlay: Int
    ) {
        baseModel.renderToBuffer(
            instance,
            poseStack,
            bufferSource,
            quadRenderType,
            triangleRenderType,
            packedLight,
            packedOverlay,
            1f,
            1f,
            1f,
            1f,
            true
        )
    }
}
