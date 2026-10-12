package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.PropertyModifier
import com.atsuishio.superbwarfare.data.attachment.AttachmentModifier
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.perk.js.PmcProxy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 自定义护木（`sbw/guns/<id>.json` 的 `HandGuard`）：三个 bool 决定哪些槽位上的配件会换护木，同时按 [modifiers] 改属性 */
@Serializable
data class HandGuardInfo(
    // 装握把/下导轨上的配件就换护木
    @SerialName("Grip")
    val grip: Boolean = false,

    // 四条导轨上装了东西就换护木
    @SerialName("Rail")
    val rail: Boolean = false,

    // 装上瞄准镜就换护木
    @SerialName("Scope")
    val scope: Boolean = false,

    @SerialName("Modifiers")
    val modifiers: List<AttachmentModifier> = emptyList(),
) : PropertyModifier<GunData, DefaultGunData> {

    /** 当前是否换了护木；模型里有没有 `custom_hand_guard` 骨骼由渲染侧另外判 */
    fun shows(data: GunData): Boolean {
        val subWeapon = data.attachment.installed().any { it.definition.subWeapon != null }
        val gripShown = grip &&
                (data.attachment.has(AttachmentType.GRIP) || data.attachment.has(AttachmentType.LOWER_RAIL) || subWeapon)
        val railShown = rail && (RAIL_SLOTS.any { data.attachment.has(it) } || subWeapon)
        val scopeShown = scope && data.attachment.has(AttachmentType.SCOPE)
        return gripShown || railShown || scopeShown
    }

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        if (!shows(modifier.data)) return

        val pmc = PmcProxy(modifier)
        modifiers.forEach { it.apply(pmc, emptyMap()) }
    }

    companion object {
        private val RAIL_SLOTS = listOf(
            AttachmentType.LOWER_RAIL,
            AttachmentType.UPPER_RAIL,
            AttachmentType.LEFT_RAIL,
            AttachmentType.RIGHT_RAIL,
        )
    }
}

/** 当前是否换上了自定义护木 */
fun GunData.showsHandGuard(): Boolean = getDefault().handGuard.shows(this)
