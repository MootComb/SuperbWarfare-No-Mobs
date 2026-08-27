package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.living.CreepingSenpaiEntity
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine

class CreepingSenpaiAnimationInstance(entity: CreepingSenpaiEntity) {
    val context: CreepingSenpaiContext = CreepingSenpaiContext(entity)
    private val stateMachine = AnimationStateMachine(CreepingSenpaiStates.INIT, context) { System.nanoTime() }

    fun tick() {
        stateMachine.tick()
        context.tick()
    }

    fun getPose(): Pose {
        return stateMachine.getPose()
    }
}
