package com.atsuishio.superbwarfare.script

import net.minecraft.world.item.ItemDisplayContext

class ScriptTransform(private val ctx: ItemDisplayContext, private val displayKey: String) {
    fun firstPerson(): Boolean = ctx.firstPerson()

    fun name(): String = ctx.name

    fun id(): Int = ctx.id.toInt()

    fun key(): String = displayKey

    override fun toString(): String = ctx.name
}
