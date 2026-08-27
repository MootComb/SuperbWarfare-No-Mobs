package com.atsuishio.superbwarfare.resource.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GunAnimation {
    @JvmField
    @SerialName("TransitionTickTime")
    var transitionTickTime: Int = 1

    // This should NOT be null or empty!
    @JvmField
    @SerialName("Idle")
    var idle: String? = null

    @JvmField
    @SerialName("Fire")
    var fire: String? = null

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

    @SerialName("Prepare")
    var prepare: String? = null

    @SerialName("Iterative")
    var iterative: String? = null

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

    @JvmField
    @SerialName("Melee")
    var melee: String? = null
}
