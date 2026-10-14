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

/** 刺刀冲锋的客户端状态 */
object GunChargeHandler {

    /** 窗口剩余 tick；`0` = 没在冲锋 */
    var chargeTicks: Int = 0
        private set

    // 冷却剩余 tick；记在玩家身上（和手里拿什么无关，换枪也不清），不用原版物品冷却
    var cooldownTicks: Int = 0
        private set

    private var chargingItem: Item? = null

    fun start(stack: ItemStack, data: GunData) {
        if (isCharging() || cooldownTicks > 0) return

        val charge = data.get(GunProp.BAYONET_CHARGE)
        if (charge.duration <= 0) return

        chargeTicks = charge.duration
        chargingItem = stack.item
        if (charge.cooldown > 0) {
            cooldownTicks = charge.cooldown
        }

        sendPacketToServer(UseActionMessage(RightClickAction.BAYONET_CHARGE))
    }

    // 每客户端 tick 递减
    fun tickCooldown() {
        if (cooldownTicks > 0) cooldownTicks--
    }

    /**
     * 推进窗口；返回**撞上的那个目标**（`null` = 这 tick 不出手）。
     *
     * 只报告，不清窗口（到点除外），真正打出去了由调用方 [reset]。返回的实体就是这次的刺击目标，
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
        val target = looking?.takeIf { MeleeQuery.isChargeTarget(player, it) }

        // 到点是本地自己收的窗口，收的那一刻就把状态同步回去
        if (chargeTicks <= 0) reset()

        return target
    }

    // 手里换成别的东西（空手/别的物品）时窗口也得收
    fun cancelIfItemChanged(stack: ItemStack) {
        if (isCharging() && stack.item !== chargingItem) reset()
    }

    fun isCharging(): Boolean = chargeTicks > 0

    // 收窗口（打完那一击、换枪、切状态、到点）；本地一收就同步给服务端，不动冷却
    fun reset() {
        val charging = isCharging()
        chargeTicks = 0
        chargingItem = null
        if (charging) {
            sendPacketToServer(UseActionMessage(RightClickAction.BAYONET_CHARGE, active = false))
        }
    }
}
