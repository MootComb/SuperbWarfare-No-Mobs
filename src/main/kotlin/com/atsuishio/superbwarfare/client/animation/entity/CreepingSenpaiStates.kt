package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition

object CreepingSenpaiStates {
    val INIT: SimpleAnimationState<CreepingSenpaiContext> = SimpleAnimationState.Builder<CreepingSenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val IDLE: SimpleAnimationState<CreepingSenpaiContext> = SimpleAnimationState.Builder<CreepingSenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val WALK: SimpleAnimationState<CreepingSenpaiContext> = SimpleAnimationState.Builder<CreepingSenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val RUN: SimpleAnimationState<CreepingSenpaiContext> = SimpleAnimationState.Builder<CreepingSenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val DIE: SimpleAnimationState<CreepingSenpaiContext> = SimpleAnimationState.Builder<CreepingSenpaiContext>()
        .evaluatePose { it.getPose() }
        .build()

    val INIT_TRANS: SimpleTransition<CreepingSenpaiContext> = SimpleTransition.Builder<CreepingSenpaiContext>()
        .predicate { true }
        .target(IDLE)
        .from(INIT)
        .afterTrigger { it.playAnimation("animation.creeping_senpai.idle", AnimationPlayType.LOOP) }
        .build()

    val TO_IDLE: SimpleTransition<CreepingSenpaiContext> = SimpleTransition.Builder<CreepingSenpaiContext>()
        .predicate { !it.isMoving() }
        .target(IDLE)
        .from(WALK, RUN)
        .afterTrigger { it.playAnimation("animation.creeping_senpai.idle", AnimationPlayType.LOOP) }
        .build()

    val TO_WALK: SimpleTransition<CreepingSenpaiContext> = SimpleTransition.Builder<CreepingSenpaiContext>()
        .predicate {
            val entity = it.entity
            val limbSwingAmount = entity.walkAnimation.speed(it.partialTick)
            (it.isMoving() || !(limbSwingAmount > -0.15f && limbSwingAmount < 0.15f)) && !entity.isAggressive
        }
        .target(WALK)
        .from(RUN, IDLE)
        .afterTrigger { it.playAnimation("animation.creeping_senpai.walk", AnimationPlayType.LOOP) }
        .build()

    val TO_RUN: SimpleTransition<CreepingSenpaiContext> = SimpleTransition.Builder<CreepingSenpaiContext>()
        .predicate {
            val entity = it.entity
            entity.isAggressive && it.isMoving()
        }
        .target(RUN)
        .from(WALK, IDLE)
        .afterTrigger { it.playAnimation("animation.creeping_senpai.run", AnimationPlayType.LOOP) }
        .build()

    val TO_DIE: SimpleTransition<CreepingSenpaiContext> = SimpleTransition.Builder<CreepingSenpaiContext>()
        .predicate { it.entity.isDeadOrDying }
        .target(DIE)
        .from(RUN, WALK, IDLE)
        .afterTrigger { it.playAnimation("animation.creeping_senpai.die", AnimationPlayType.PLAY_ONCE_HOLD) }
        .build()
}
