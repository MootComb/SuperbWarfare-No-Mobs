package com.atsuishio.superbwarfare.resource.model

import com.atsuishio.superbwarfare.client.model.shell.BedrockShellModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

object ShellModelReloadListener : BedrockModelReloadListener<BedrockShellModel>("models/bedrock/shell") {
    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        this.models.clear()
        this.animations.clear()

        map.forEach { (location, pojo) ->
            this.models[location] = BedrockShellModel(TreeBedrockModel.bake(pojo))
        }
    }
}
