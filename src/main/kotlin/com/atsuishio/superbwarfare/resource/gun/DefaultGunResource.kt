package com.atsuishio.superbwarfare.resource.gun

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.ModColor
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.resource.ModelResource
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joml.Vector3f

@Serializable
class DefaultGunResource : IDBasedData<DefaultGunResource> {
    @Transient
    @kotlin.jvm.Transient
    private var id = ""

    override fun getId(): String {
        return this.id
    }

    override fun setId(id: String) {
        this.id = id
    }

    @SerialName("Icon")
    var icon: String = loc("textures/gun_icon/default_icon.png").toString()

    @SerialName("SlotIcon")
    var slotIcon: String = ""

    @SerialName("ItemDisplay")
    var itemDisplay: MutableMap<String, ItemDisplayInfo> = hashMapOf()

    @SerialName("Model")
    var modelValue: ModelResource? = ModelResource()

    fun getModel(): ModelResource {
        return if (modelValue == null) ModelResource() else modelValue!!
    }

    @JvmField
    @SerialName("Animation")
    var animation: GunAnimation? = GunAnimation()

    @JvmField
    @SerialName("UseOldHandRenderer")
    var useOldHandRenderer: Boolean = false

    @SerialName("FlarePosition")
    var flarePosition: SerializedVec3? = null

    @SerialName("FlareSize")
    var flareSize: Float = 1f

    @SerialName("HideCrosshairWhenZoom")
    var hideCrosshairWhenZoom: Boolean = true

    @SerialName("EnergyBarColor")
    var energyBarColor: ModColor = ModColor(0x95E9FF)

    @SerialName("TriggerSound")
    var triggerSound: SerializedSoundEvent = ModSounds.TRIGGER_CLICK.get()

    @SerialName("DischargeSound")
    var dischargeSound: SerializedSoundEvent? = null

    @SerialName("EjectShell")
    var ejectShell: Boolean = false

    @SerialName("CanZoom")
    var canZoom: Boolean = true

    @SerialName("SprintOffset")
    var sprintOffset: SerializedVector3f = Vector3f(0f, 0f, 0f)

    @Serializable
    class ItemDisplayInfo {
        @SerialName("translation")
        var translation: SerializedVector3f = Vector3f(0f, 0f, 0f)

        @SerialName("rotation")
        var rotation: SerializedVector3f = Vector3f(0f, 0f, 0f)

        @SerialName("scale")
        var scale: SerializedVector3f = Vector3f(0f, 0f, 0f)
    }
}
