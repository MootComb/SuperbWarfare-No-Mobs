package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.capability.player.PlayerSpinState
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class WeaponSpinMessage(val spinning: Boolean) : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        val stack = player.mainHandItem

        val valid = spinning
                && !player.isSpectator
                && GunItem.isHeldWeapon(stack)

        PlayerSpinState.set(player, valid)
    }
}
