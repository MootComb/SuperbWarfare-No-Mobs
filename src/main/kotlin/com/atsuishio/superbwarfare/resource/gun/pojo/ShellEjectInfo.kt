package com.atsuishio.superbwarfare.resource.gun.pojo

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.joml.Vector3f

@Serializable
class ShellEjectInfo {
    @JvmField
    @SerialName("BoneName")
    var boneName: String = "shell"

    @JvmField
    @SerialName("ShellModel")
    var shellModel: SerializedResourceLocation? = null

    @JvmField
    @SerialName("ShellTexture")
    var shellTexture: SerializedResourceLocation? = null

    @JvmField
    @SerialName("Size")
    var size: Float = 1f

    @JvmField
    @SerialName("InitialVelocity")
    var initialVelocity: SerializedVector3f = Vector3f(1.6f, 0.9f, 0.25f)

    @JvmField
    @SerialName("RandomVelocity")
    var randomVelocity: SerializedVector3f = Vector3f(0.4f, 0.35f, 0.15f)

    @JvmField
    @SerialName("Acceleration")
    var acceleration: SerializedVector3f = Vector3f(0f, -18f, 0f)

    @JvmField
    @SerialName("AngularVelocity")
    var angularVelocity: SerializedVector3f = Vector3f(-1800f, -2000f, 240f)

    @JvmField
    @SerialName("RandomAngle")
    var randomAngle: SerializedVector3f = Vector3f(0f, 0f, 0f)

    @JvmField
    @SerialName("LivingTime")
    var livingTime: Float = 0.9f

    @JvmField
    @SerialName("MaxActive")
    var maxActive: Int = 32
}