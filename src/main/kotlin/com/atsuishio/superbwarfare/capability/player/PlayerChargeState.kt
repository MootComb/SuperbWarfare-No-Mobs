package com.atsuishio.superbwarfare.capability.player

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.CapabilityWrapper
import com.atsuishio.superbwarfare.capability.ModCapabilities
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability

// 刺刀冲锋状态：冲锋者的客户端在本地开/关的那一刻发报文，服务端照写；给别人算姿势用。不落盘
@Serializable
data class PlayerChargeState(
    val charging: Boolean = false,
) {
    companion object {
        val ID: ResourceLocation = loc("player_charge_state")

        @JvmStatic
        fun getOrDefault(entity: Entity): PlayerChargeState = ModCapabilities.PLAYER_CHARGE_STATE.get(entity)

        // 渲染路径每帧都会问，不走会新造默认值的那条路
        @JvmStatic
        fun isCharging(entity: Entity): Boolean =
            ModCapabilities.PLAYER_CHARGE_STATE.getOrNull(entity)?.charging == true

        @JvmStatic
        fun modify(entity: Entity, mapper: (PlayerChargeState) -> PlayerChargeState) {
            ModCapabilities.PLAYER_CHARGE_STATE.update(entity, mapper)
        }
    }

    // 只为了让 Forge 有一个唯一的 capability 实现类而存在，见 CapabilityWrapper
    @AutoRegisterCapability
    class Holder : CapabilityWrapper<PlayerChargeState>(
        serializer(),
        PlayerChargeState(),
        persistent = false,
    )
}
