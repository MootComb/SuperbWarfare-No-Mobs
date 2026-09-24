package com.atsuishio.superbwarfare.serialization.kserializer

import com.atsuishio.superbwarfare.tools.toGson
import com.atsuishio.superbwarfare.tools.toKxJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import net.minecraft.world.item.crafting.Ingredient

/**
 * [Ingredient] 的 kotlinx 序列化封装，供配方数据类直接声明 `Ingredient` 字段。
 *
 * Ingredient 的 JSON 形态（`{"item": ...}` / `{"tag": ...}` / 对象数组 / Forge 自定义 `type`）
 * 由原版和 Forge 共同定义，自己重写一份容易丢掉 `forge:nbt` 之类的自定义 Ingredient，
 * 所以这里只在**边界**处借用它们的解析结果，配方数据本身完全走 kotlinx。
 */
typealias SerializedIngredient = @Serializable(IngredientSerializer::class) Ingredient

object IngredientSerializer : KSerializer<Ingredient> {
    override val descriptor = buildClassSerialDescriptor("Ingredient")

    @Suppress("DEPRECATION")
    override fun serialize(encoder: Encoder, value: Ingredient) {
        require(encoder is JsonEncoder) { "Only JsonEncoder is supported!" }
        encoder.encodeJsonElement(value.toJson().toKxJson())
    }

    override fun deserialize(decoder: Decoder): Ingredient {
        require(decoder is JsonDecoder) { "Only JsonDecoder is supported!" }

        val element = decoder.decodeJsonElement()
        if (element is JsonNull) return Ingredient.EMPTY

        return Ingredient.fromJson(element.toGson())
    }
}
