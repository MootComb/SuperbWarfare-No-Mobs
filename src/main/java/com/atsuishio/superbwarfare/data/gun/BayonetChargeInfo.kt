package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 刺刀冲锋（`RightClickAction: "BayonetCharge"`）的参数，和开火蓄力的 `ChargeAction` 无关 */
@Serializable
data class BayonetChargeInfo(
    @SerialName("Duration")
    val duration: Int = 180,

    /** 迅捷等级（amplifier，0 = 迅捷 I） */
    @SerialName("Speed")
    val speed: Int = 2,

    @SerialName("Cooldown")
    val cooldown: Int = 300,

    @SerialName("Sound")
    val sound: SerializedSoundEvent? = null,

    /** 撞人那一下的挥击时长（tick）：非正数 = 跟 `MeleeActions` 那一段的 `Duration` 走 */
    @SerialName("StrikeDuration")
    val strikeDuration: Int = -1,

    /** 撞人那一下从挥击开始算第几 tick 结算：负数 = 跟那一段的 `HitTime` 走 */
    @SerialName("StrikeHitTime")
    val strikeHitTime: Int = -1,
)
