package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 配件皮肤（`AttachmentDefinition.skins` 的一项，写在 `sbw/attachments/<id>.json` 的 `Skins` 里）。
 *
 * 与载具皮肤的区别是**可以换模型**：不写 [model] 就用配件本体的 `Model`，
 * 不写 [texture] 就用配件本体的 `Texture`，所以"只换贴图"和"只换模型"都只要写一个字段。
 */
@Serializable
data class AttachmentSkinInfo(
    @SerialName("Id")
    val id: String = "",

    @SerialName("Name")
    val name: String = "",

    @SerialName("Description")
    val description: String = "",

    @SerialName("Model")
    val model: SerializedResourceLocation? = null,

    @SerialName("Texture")
    val texture: SerializedResourceLocation? = null,

    @SerialName("Priority")
    val priority: Int = 0,
)
