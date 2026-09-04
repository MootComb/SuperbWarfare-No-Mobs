package com.atsuishio.superbwarfare.item.gun

import com.atsuishio.superbwarfare.client.PoseTool
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent

// TODO 替换掉之前的GunGeoItem，给这个的V2去掉
open class GeoGunItemV2(properties: Properties) : GunItem(properties) {

    @EventBusSubscriber
    companion object {
        @SubscribeEvent
        private fun registerGunExtensions(event: RegisterClientExtensionsEvent) {
            for (item in BuiltInRegistries.ITEM.filterIsInstance<GeoGunItemV2>()) {
                event.registerItem(object : IClientItemExtensions {
                    private val renderer by lazy { GeoGunRenderer() }

                    override fun getCustomRenderer() = renderer

                    override fun getArmPose(
                        entityLiving: LivingEntity,
                        hand: InteractionHand,
                        itemStack: ItemStack
                    ) = PoseTool.pose(entityLiving, hand, itemStack)
                }, item)
            }
        }

    }
}