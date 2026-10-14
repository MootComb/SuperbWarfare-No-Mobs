package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec2
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec2

/** 导轨允许的实例偏移范围：[最小值, 最大值]，单位为 Bedrock 模型单位（1/16 方块）。 */
@Serializable
data class RailInfo(
    @SerialName("Scope")
    val scope: SerializedVec2 = Vec2(-3.0f, 3.0f),
    @SerialName("Grip")
    val grip: SerializedVec2 = Vec2(-0.5f, 0.5f),
    @SerialName("UpperRail")
    val upperRail: SerializedVec2 = Vec2(-0.5f, 0.5f),
    @SerialName("LowerRail")
    val lowerRail: SerializedVec2 = Vec2(-0.5f, 0.5f),
    @SerialName("LeftRail")
    val leftRail: SerializedVec2 = Vec2(-0.5f, 0.5f),
    @SerialName("RightRail")
    val rightRail: SerializedVec2 = Vec2(-0.5f, 0.5f),
) {
    /** 不属于导轨的槽位返回 null，由完全自由改装的规则提供范围。 */
    fun offsetRange(type: AttachmentType): ClosedFloatingPointRange<Double>? {
        val vector = vectorOf(type) ?: return null
        val valid = if (vector.x.isFinite() && vector.y.isFinite()) vector else RailInfo().vectorOf(type)!!
        // 与 SerializedVec2 的十进制表示保持一致，避免 0.1f 转 Double 后暴露浮点尾差。
        val first = valid.x.toString().toDouble()
        val second = valid.y.toString().toDouble()
        return minOf(first, second)..maxOf(first, second)
    }

    private fun vectorOf(type: AttachmentType): Vec2? = when (type) {
        AttachmentType.SCOPE -> scope
        AttachmentType.GRIP -> grip
        AttachmentType.UPPER_RAIL -> upperRail
        AttachmentType.LOWER_RAIL -> lowerRail
        AttachmentType.LEFT_RAIL -> leftRail
        AttachmentType.RIGHT_RAIL -> rightRail
        else -> null
    }
}
