package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.renderer.item.KnifeM1917Renderer
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.tiers.ModItemTier
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.world.item.SwordItem
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("knife_m1917")
class KnifeM1917Item : SwordItem(
    ModItemTier.STEEL, 4, -1.8F, Properties().durability(1600)
) {

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        consumer.accept(object : IClientItemExtensions {
            private val renderer by lazy { KnifeM1917Renderer(mc.blockEntityRenderDispatcher, mc.entityModels) }
            override fun getCustomRenderer() = renderer
        })
    }
}
