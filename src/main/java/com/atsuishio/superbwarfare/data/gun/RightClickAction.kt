package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RightClickAction {
    @SerialName("Aim")
    AIM,

    @SerialName("HeavyStrike")
    HEAVY_STRIKE,

    @SerialName("BayonetCharge")
    BAYONET_CHARGE,
}
