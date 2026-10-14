package com.atsuishio.superbwarfare.client.screens

import java.util.*

internal object AttachmentColorInput {
    private val HEX_DRAFT = Regex("#?[0-9a-fA-F]{0,6}")
    private val HEX_COLOR = Regex("#?[0-9a-fA-F]{6}")

    fun isDraft(value: String) = HEX_DRAFT.matches(value)

    fun parse(value: String): Int? =
        value.takeIf { HEX_COLOR.matches(it) }?.removePrefix("#")?.toIntOrNull(16)

    fun format(color: Int): String = String.format(Locale.ROOT, "#%06X", color and 0xFFFFFF)

    fun shift(property: String): Int? = when (property) {
        "red" -> 16
        "green" -> 8
        "blue" -> 0
        else -> null
    }

    fun channel(color: Int, shift: Int) = (color ushr shift) and 255

    fun withChannel(color: Int, shift: Int, value: Int): Int =
        (color and (255 shl shift).inv()) or (value.coerceIn(0, 255) shl shift)
}
