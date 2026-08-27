package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType

enum class GunAnimationState(val playType: AnimationPlayType) {
    IDLE(AnimationPlayType.LOOP),
    EDIT(AnimationPlayType.PLAY_ONCE_HOLD),
    BOLT(AnimationPlayType.PLAY_ONCE_HOLD),
    RELOAD(AnimationPlayType.PLAY_ONCE_HOLD),
    RELOAD_NORMAL(AnimationPlayType.PLAY_ONCE_HOLD),
    RELOAD_EMPTY(AnimationPlayType.PLAY_ONCE_HOLD),
    MELEE(AnimationPlayType.PLAY_ONCE_HOLD),
    FIRE(AnimationPlayType.LOOP),
    RUN(AnimationPlayType.LOOP)
}
