package com.atsuishio.superbwarfare.data.gun.value

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AttachmentType(typeName: String) {
    @SerialName("Scope")
    SCOPE("Scope"),

    @SerialName("Magazine")
    MAGAZINE("Magazine"),

    @SerialName("Barrel")
    BARREL("Barrel"),

    @SerialName("Stock")
    STOCK("Stock"),

    @SerialName("Grip")
    GRIP("Grip");

    val attachmentName: String = typeName
}
