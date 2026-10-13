package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.RightClickAction
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.UseActionMessage
import com.atsuishio.superbwarfare.tools.MeleeQuery
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/** 刺刀冲锋（`RightClickAction: "BayonetCharge"`）的客户端状态 */
object GunChargeHandler {

    /** 窗口剩余 tick；`0` = 没在冲锋 */
    var chargeTicks: Int = 0
        private set

    private var chargingItem: Item? = null

    fun start(player: Player, stack: ItemStack, data: GunData) {
        if (data.get(GunProp.RIGHT_CLICK_ACTION) != RightClickAction.BAYONET_CHARGE) return
        if (player.cooldowns.isOnCooldown(stack.item)) return

        val charge = data.get(GunProp.BAYONET_CHARGE)
        if (charge.duration <= 0) return

        chargeTicks = charge.duration
        chargingItem = stack.item
        if (charge.cooldown > 0) {
            player.cooldowns.addCooldown(stack.item, charge.cooldown)
        }

        sendPacketToServer(UseActionMessage(RightClickAction.BAYONET_CHARGE))
    }

    /**
     * 推进窗口；返回**撞上的那个目标**（`null` = 这 tick 不出手）。
     *
     * 只报告，不清窗口，真正打出去了由调用方 [reset]。返回的实体就是这次的刺击目标，
     * 结算那一下会强行把它算进命中里（见 `MeleeQuery.resolve` 的 `forced`）。
     *
     * 判据与结算侧同一个（[MeleeQuery.isChargeTarget]）：戳不了的东西不消耗这一击，
     * 窗口继续跑，免得白打一下空。
     */
    fun tick(player: Player, stack: ItemStack): Entity? {
        if (chargeTicks <= 0) return null

        // 换枪就作废（窗口跟着上一把枪走）
        if (!GunItem.isHeldWeapon(stack) || stack.item !== chargingItem) {
            reset()
            return null
        }

        chargeTicks--

        // 撞到东西才打；窗口内可以一直等
        val looking = OverlayTraceHandler.playerReachEntity
        return looking?.takeIf { MeleeQuery.isChargeTarget(player, it) }
    }

    fun isCharging(): Boolean = chargeTicks > 0

    fun reset() {
        chargeTicks = 0
        chargingItem = null
    }
}
