package com.atsuishio.superbwarfare.resource.gun.pojo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SmokeInfo {
    // 初始大小
    @JvmField
    @SerialName("Size")
    var size: Float = 0.3f

    // 增长量
    @JvmField
    @SerialName("Growth")
    var growth: Float = 0.3f

    // 持续时间
    @JvmField
    @SerialName("Lifetime")
    var lifetime: Float = 0.3f

    // 扩散速度
    @JvmField
    @SerialName("Speed")
    var speed: Float = 0.6f

    // 粒子数量
    @JvmField
    @SerialName("Count")
    var count: Float = 4f

    // 透明度
    @JvmField
    @SerialName("Opacity")
    var opacity: Float = 1f

    // 速度衰减
    @JvmField
    @SerialName("Drag")
    var drag: Float = 2f

    @JvmField
    @SerialName("RandomSize")
    var randomSize: Float = 0.4f

    @JvmField
    @SerialName("RandomGrowth")
    var randomGrowth: Float = 0.5f

    @JvmField
    @SerialName("RandomLifetime")
    var randomLifetime: Float = 0.3f

    @JvmField
    @SerialName("RandomSpeed")
    var randomSpeed: Float = 0.6f

    @JvmField
    @SerialName("RandomCount")
    var randomCount: Float = 0.3f

    @JvmField
    @SerialName("RandomOpacity")
    var randomOpacity: Float = 0.3f
}