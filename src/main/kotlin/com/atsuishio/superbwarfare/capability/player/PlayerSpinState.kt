package com.atsuishio.superbwarfare.capability.player

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.CapabilityWrapper
import com.atsuishio.superbwarfare.capability.ModCapabilities
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability

@Serializable
data class PlayerSpinState(
    val spinning: Boolean = false,
) {
    companion object {
        val ID: ResourceLocation = loc("player_spin_state")

        @JvmStatic
        fun isSpinning(entity: Entity): Boolean =
            ModCapabilities.PLAYER_SPIN_STATE.getOrNull(entity)?.spinning == true

        @JvmStatic
        fun set(entity: Entity, spinning: Boolean) =
            ModCapabilities.PLAYER_SPIN_STATE.set(entity, PlayerSpinState(spinning))
    }

    @AutoRegisterCapability
    class Holder : CapabilityWrapper<PlayerSpinState>(
        serializer(),
        PlayerSpinState(),
        persistent = false,
    )
}
