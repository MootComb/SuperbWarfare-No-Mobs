package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.renderer.item.KnifeSeitengewehr84Renderer
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.tools.mc
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("knife_seitengewehr_84")
class KnifeSeitengewehr84Item : KnifeItem() {

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        consumer.accept(object : IClientItemExtensions {
            private val renderer by lazy {
                KnifeSeitengewehr84Renderer(mc.blockEntityRenderDispatcher, mc.entityModels)
            }

            override fun getCustomRenderer() = renderer
        })
    }
}
