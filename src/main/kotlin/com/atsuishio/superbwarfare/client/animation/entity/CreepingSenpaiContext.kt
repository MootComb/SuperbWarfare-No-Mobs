package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.CreepingSenpaiEntity
import kotlin.math.abs

class CreepingSenpaiContext(entity: CreepingSenpaiEntity) : BasicEntityContext<CreepingSenpaiEntity>(entity, ANIM) {
    companion object {
        val ANIM = loc("animations/bedrock/entity/creeping_senpai.animation.json")
    }

    fun isMoving(): Boolean {
        val velocity = entity.deltaMovement
        val avgVelocity = (abs(velocity.x) + abs(velocity.y) + abs(velocity.z)).toFloat() / 3f
        return avgVelocity > 0.015f
    }
}
