package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.capability.player.PlayerChargeState
import com.atsuishio.superbwarfare.data.gun.BayonetChargeInfo
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import java.util.*

// 冲锋状态的服务端一半：客户端在本地开/关的那一刻发报文过来，这里只负责照写 + 加/撤移速
object ChargeWindowHandler {

    // 必须和原版迅捷效果的 UUID 错开，否则会把真正的迅捷顶掉
    private val SPEED_MODIFIER: UUID = UUID.fromString("6d2a5b9e-3f47-4c81-9a52-b1e7c04f8d33")

    // 移速加成和原版迅捷同源：等级 × 20%
    fun start(player: Player, charge: BayonetChargeInfo) {
        PlayerChargeState.modify(player) { it.copy(charging = true) }
        player.getAttribute(Attributes.MOVEMENT_SPEED)?.addTransientModifier(
            AttributeModifier(
                SPEED_MODIFIER, "BayonetCharge",
                0.2 * (charge.speed + 1), AttributeModifier.Operation.MULTIPLY_TOTAL
            )
        )
    }

    fun clear(player: Player) {
        PlayerChargeState.modify(player) { it.copy(charging = false) }
        player.getAttribute(Attributes.MOVEMENT_SPEED)?.removeModifier(SPEED_MODIFIER)
    }
}
