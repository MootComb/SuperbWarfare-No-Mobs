package com.atsuishio.superbwarfare.recipe.vehicle

import com.atsuishio.superbwarfare.data.DataLoader.processValue
import com.atsuishio.superbwarfare.data.SingleOrList
import com.atsuishio.superbwarfare.data.StringOrObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleAssemblingRecipeData(
    @SerialName("inputs")
    @get:JvmName("inputs")
    val inputs: SingleOrList<StringOrObject<VehicleAssemblingIngredient>>? = null,

    @SerialName("result")
    val result: VehicleAssemblingResult? = null,

    @SerialName("category")
    val category: String = "empty",
) {
    @Suppress("UNCHECKED_CAST")
    fun getInputs(): MutableList<VehicleAssemblingIngredient>? {
        return processValue(inputs) as MutableList<VehicleAssemblingIngredient>?
    }
}
