package com.atsuishio.superbwarfare.item.gun.special

import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.world.item.Rarity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent

class NailGunItem : GunItem(Properties().rarity(Rarity.RARE)) {

    @EventBusSubscriber
    companion object {
        @SubscribeEvent
        fun initializeClient(event: RegisterClientExtensionsEvent) {
            event.registerItem(object : IClientItemExtensions {
                private val renderer by lazy { GeoGunRenderer() }

                override fun getCustomRenderer() = renderer
            }, ModItems.NAIL_GUN.get())
        }
    }
}