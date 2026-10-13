package com.atsuishio.superbwarfare.resource.gun

import com.atsuishio.superbwarfare.data.SingleOrList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GunAnimation {
    // This should NOT be null or empty!
    @JvmField
    @SerialName("Idle")
    var idle: String? = null

    @JvmField
    @SerialName("Fire")
    var fire: String? = null

    // 装了枪托时替换 Fire
    @JvmField
    @SerialName("FireStock")
    var fireStock: String? = null

    // 换上自定义护木时，下面几条的替身
    @JvmField
    @SerialName("IdleHandGuard")
    var idleHandGuard: String? = null

    @JvmField
    @SerialName("FireHandGuard")
    var fireHandGuard: String? = null

    @JvmField
    @SerialName("EditHandGuard")
    var editHandGuard: String? = null

    @JvmField
    @SerialName("ReloadHandGuard")
    var reloadHandGuard: String? = null

    @JvmField
    @SerialName("ReloadNormalHandGuard")
    var reloadNormalHandGuard: String? = null

    @JvmField
    @SerialName("ReloadEmptyHandGuard")
    var reloadEmptyHandGuard: String? = null

    @JvmField
    @SerialName("ChangeFireMode")
    var changeFireMode: String? = null

    @JvmField
    @SerialName("FireModes")
    var fireModes: List<String> = emptyList()

    // Reload > ReloadNormal | ReloadEmpty
    @JvmField
    @SerialName("Reload")
    var reload: String? = null

    @JvmField
    @SerialName("ReloadNormal")
    var reloadNormal: String? = null

    @JvmField
    @SerialName("ReloadEmpty")
    var reloadEmpty: String? = null

    @JvmField
    @SerialName("ReloadNormalDrum")
    var reloadNormalDrum: String? = null

    @JvmField
    @SerialName("ReloadEmptyDrum")
    var reloadEmptyDrum: String? = null

    @JvmField
    @SerialName("IdleDeploy")
    var idleDeploy: String? = null

    @JvmField
    @SerialName("ReloadNormalDeploy")
    var reloadNormalDeploy: String? = null

    @JvmField
    @SerialName("ReloadEmptyDeploy")
    var reloadEmptyDeploy: String? = null

    @JvmField
    @SerialName("HoldOpen")
    var holdOpen: String? = null

    @JvmField
    @SerialName("CloseStrike")
    var closeStrike: String? = null

    // 加特林式枪管旋转
    @JvmField
    @SerialName("Hold")
    var hold: String? = null

    // 常驻循环动画，任何状态下都播，垫在所有层之下
    @JvmField
    @SerialName("Loop")
    var loop: String? = null

    // 循环开火动画
    @JvmField
    @SerialName("FireLoop")
    var fireLoop: String? = null

    /** [fireLoop] 的淡入时长，单位秒 */
    @JvmField
    @SerialName("FireLoopFadeIn")
    var fireLoopFadeIn: Float = 0f

    /** [fireLoop] 的淡出时长，单位**秒**。松手之后这一层按它平滑退回当前姿态 */
    @JvmField
    @SerialName("FireLoopFadeOut")
    var fireLoopFadeOut: Float = 0f

    @JvmField
    @SerialName("Prepare")
    var prepare: String? = null

    @JvmField
    @SerialName("PrepareLoad")
    var prepareLoad: String? = null

    @JvmField
    @SerialName("Iterative")
    var iterative: String? = null

    @JvmField
    @SerialName("Finish")
    var finish: String? = null

    @JvmField
    @SerialName("Edit")
    var edit: String? = null

    @JvmField
    @SerialName("Bolt")
    var bolt: String? = null

    @JvmField
    @SerialName("Run")
    var run: String? = null

    // 刺刀冲锋（`RightClickAction: "BayonetCharge"`）窗口内的循环动画，冲锋时顶掉 [run]
    @JvmField
    @SerialName("BayonetCharge")
    var bayonetCharge: String? = null

    // 冲锋撞到目标那一下的挥击动画，只替换本段的 [melee]（时长/判定/效果仍走动作表）
    @JvmField
    @SerialName("BayonetChargeHit")
    var bayonetChargeHit: String? = null

    // 近战动画 clip 名
    @JvmField
    @SerialName("Melee")
    var melee: SingleOrList<String>? = null

    /**
     * 换弹 clip 名
     */
    fun reloadClip(emptyReload: Boolean = true, drumLevel: Boolean = false): String? {
        if (!emptyReload) {
            if (drumLevel) reloadNormalDrum?.let { return it }
            return reloadNormal
        }
        if (drumLevel) reloadEmptyDrum?.let { return it }
        return reloadEmpty ?: reload
    }

    @JvmField
    @SerialName("Charge")
    var charge: String? = null
}
