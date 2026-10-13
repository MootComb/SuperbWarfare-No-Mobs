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

        // 复制发生在 PlayerList#respawn 的初始同步之前，客户端会收到复制后的数据；
        // 而重生后客户端不会重建本地玩家实体，所以还要显式标记一次，让框架重推。
        ModCapabilities.PLAYER_VARIABLE.set(event.entity, original.copy())
        ModCapabilities.PLAYER_VARIABLE.markDirty(event.entity)
    }
}
