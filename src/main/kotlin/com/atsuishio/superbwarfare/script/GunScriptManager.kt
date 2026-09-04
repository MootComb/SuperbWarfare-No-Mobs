package com.atsuishio.superbwarfare.script

import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.mozillaa.javascript.Context
import org.mozillaa.javascript.Script
import org.mozillaa.javascript.Scriptable
import org.mozillaa.javascript.ScriptableObject

object GunScriptManager {
    val RHINO_CONTEXT: Context = Context.enter()
    val SHARED_SCOPE: ScriptableObject = RHINO_CONTEXT.initStandardObjects()

    class ScriptFunction(val script: Script, val scope: Scriptable)

    fun invokeTransform(
        scriptFunc: ScriptFunction,
        stack: ItemStack,
        model: GeoGunModel,
        transformType: ItemDisplayContext,
        partialTick: Float,
        renderer: Any
    ) {
        val func = scriptFunc.scope.get("transformCustomModelPart", scriptFunc.scope)
        if (func is JSFunction) {
            func.call(
                RHINO_CONTEXT,
                scriptFunc.scope,
                scriptFunc.scope,
                arrayOf(stack, model, transformType, partialTick, renderer)
            )
        }
    }
}
