package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 刺刀冲锋的参数，和开火蓄力的 `ChargeAction` 无关；没写这块（`Duration <= 0`）= 这把枪不能冲锋 */
@Serializable
data class BayonetChargeInfo(
    @SerialName("Duration")
    val duration: Int = 0,

    /** 冲锋期间的移速加成等级，和原版迅捷一个算法（每级 +20%）；0 = +20% */
    @SerialName("Speed")
    val speed: Int = 2,

    /** 再次冲锋前的冷却（tick）：记在玩家身上，不用原版物品冷却 */
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

    /** 撞人那一下的绝对伤害（不乘 `MeleeDamage`、不吃配件加成）：非正数 = 跟那一段的伤害走 */
    @SerialName("StrikeDamage")
    val strikeDamage: Double = -1.0,

    /** 默认需要安装刺刀型枪口配件；自带刺刀或冲锋能力的武器可显式关闭 */
    @SerialName("RequiresBayonet")
    val requiresBayonet: Boolean = true,
) {
    /** 客户端启动、服务端启动和冲锋伤害结算共用的资格检查。 */
    fun canCharge(data: GunData): Boolean = duration > 0 &&
            (!requiresBayonet || data.attachment.id(AttachmentType.MUZZLE)
                ?.let { AttachmentDefinition.from(it)?.isBayonet } == true)
}
