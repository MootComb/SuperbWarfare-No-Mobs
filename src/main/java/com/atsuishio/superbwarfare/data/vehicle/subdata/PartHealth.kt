package com.atsuishio.superbwarfare.data.vehicle.subdata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PartHealth {
    @SerialName("Turret")
    var turret: Float = 50f

    @SerialName("LeftWheel")
    var leftWheel: Float = 50f

    @SerialName("RightWheel")
    var rightWheel: Float = 50f

    @SerialName("MainEngine")
    var mainEngine: Float = 50f

    @SerialName("SubEngine")
    var subEngine: Float = 50f
}