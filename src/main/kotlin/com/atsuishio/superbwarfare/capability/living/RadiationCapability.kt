package com.atsuishio.superbwarfare.capability.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.CapabilityWrapper
import com.atsuishio.superbwarfare.capability.ModCapabilities
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability

/**
 * 辐射剂量。
 *
 * 只描述数据；存档读写与网络同步由 [CapabilityWrapper] 代劳，见 [ModCapabilities.RADIATION]。
 */
@Serializable
data class RadiationCapability(
    @SerialName("SbwRadiationDosage")
    val dosage: Float = 0f,
) {
    companion object {
        val ID: ResourceLocation = loc("radiation_capability")

        const val MAX_DOSAGE = 20000f

        @JvmStatic
        fun get(entity: Entity): RadiationCapability = ModCapabilities.RADIATION.get(entity)

        @JvmStatic
        fun getOrNull(entity: Entity): RadiationCapability? = ModCapabilities.RADIATION.getOrNull(entity)

        @JvmStatic
        fun getDosage(entity: Entity): Float = get(entity).dosage

        @JvmStatic
        fun setDosage(entity: Entity, value: Float): Float {
            val clamped = value.coerceIn(0f, MAX_DOSAGE)
            ModCapabilities.RADIATION.set(entity, RadiationCapability(clamped))
            return clamped
        }

        @JvmStatic
        fun addDosage(entity: Entity, amount: Float): Float = setDosage(entity, getDosage(entity) + amount)
    }

    /** 只为了让 Forge 有一个唯一的 capability 实现类而存在，见 [CapabilityWrapper]。 */
    @AutoRegisterCapability
    class Holder : CapabilityWrapper<RadiationCapability>(
        serializer(),
        RadiationCapability(),
    )
}
