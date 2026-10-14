package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentAdjustments
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

/** 实例调整：携带枪和配件 id，避免切换物品或更换配件后把旧滑块值写到新目标上。 */
@RegisterPacket
@Serializable
data class AttachmentAdjustMessage(
    val gunId: String,
    val gunUuid: String?,
    val hotbarSlot: Int,
    val slot: String,
    val attachmentId: String,
    val rotation: Double,
    val offset: Double,
    val reset: Boolean = false,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (player.inventory.selected != hotbarSlot) return
        val stack = player.mainHandItem
        if (stack.item !is GunItem || !rotation.isFinite() || !offset.isFinite()) return
        val type = AttachmentType.entries.firstOrNull { it.attachmentName == slot } ?: return
        val data = GunData.from(stack)
        if (data.getDefault().itemId != gunId || data.uuid?.toString() != gunUuid ||
            data.attachment.id(type)?.toString() != attachmentId) return
        if (type == AttachmentType.MAGAZINE) return
        // 重置同时清除两个实例属性，包含当前模式隐藏的那一个；普通调整仍遵守槽位限制。
        if (reset) {
            data.attachment.setRotation(type, 0.0)
            data.attachment.setOffset(type, 0.0)
            data.save()
            return
        }
        if (!AttachmentAdjustments.canRotate(type) && !AttachmentAdjustments.canOffset(type)) return

        if (AttachmentAdjustments.canRotate(type)) {
            data.attachment.setRotation(type, rotation)
        }
        if (AttachmentAdjustments.canOffset(type)) {
            val range = AttachmentAdjustments.offsetRange(data, type)
            data.attachment.setOffset(type, offset.coerceIn(range.start, range.endInclusive))
        }
        data.save()
    }
}
