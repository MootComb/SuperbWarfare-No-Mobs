package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.RightClickAction
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects

/** 右键行为（`RightClickAction`）里需要服务端权威的那部分 */
@RegisterPacket
@Serializable
data class UseActionMessage(val action: RightClickAction) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return

        val data = GunData.from(stack)
        if (data.get(GunProp.RIGHT_CLICK_ACTION) != action) return

        if (action == RightClickAction.BAYONET_CHARGE) {
            val charge = data.get(GunProp.BAYONET_CHARGE)
            if (charge.duration <= 0) return
            if (player.cooldowns.isOnCooldown(stack.item)) return

            player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SPEED, charge.duration, charge.speed))
            charge.sound?.let {
                player.level().playSound(null, player.onPos, it, SoundSource.PLAYERS, 2f, 1f)
            }
            if (charge.cooldown > 0) {
                player.cooldowns.addCooldown(stack.item, charge.cooldown)
            }
        }
    }
}
