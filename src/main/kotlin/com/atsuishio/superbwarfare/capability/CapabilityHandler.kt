package com.atsuishio.superbwarfare.capability

import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import net.minecraft.world.entity.Entity
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

@EventBusSubscriber
object CapabilityHandler {

    /**
     * 按 [ModCapabilities.ALL] 中声明的挂载目标批量挂载。
     *
     * 新增 capability 不需要动这里，只要在 [ModCapabilities] 里登记。
     */
    @SubscribeEvent
    fun registerCapabilities(event: AttachCapabilitiesEvent<Entity>) {
        val entity = event.getObject()

        for (capability in ModCapabilities.ALL) {
            if (!capability.target.accepts(entity)) continue

            event.addCapability(capability.id, capability.createProvider())
        }
    }

    @SubscribeEvent
    fun clonePlayer(event: PlayerEvent.Clone) {
        event.original.revive()
        if (event.entity.level().isClientSide()) return

        val original = PlayerVariable.getOrDefault(event.original)
        ModCapabilities.PLAYER_VARIABLE.set(event.entity, original.copy())
        ModCapabilities.PLAYER_VARIABLE.markDirty(event.entity)

        ModCapabilities.INFINITE_AMMO.set(event.entity, ModCapabilities.INFINITE_AMMO.get(event.original))
        ModCapabilities.INFINITE_AMMO.markDirty(event.entity)
    }
}
