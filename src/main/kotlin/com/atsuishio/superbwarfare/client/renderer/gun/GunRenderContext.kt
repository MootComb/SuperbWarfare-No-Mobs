package com.atsuishio.superbwarfare.client.renderer.gun

import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity

object GunRenderContext {
    private class Frame(val entity: LivingEntity, val arm: HumanoidArm)

    private val frames = ArrayDeque<Frame>()

    @JvmStatic
    fun push(entity: LivingEntity, arm: HumanoidArm) {
        frames.addLast(Frame(entity, arm))
    }

    @JvmStatic
    fun pop() {
        frames.removeLastOrNull()
    }

    @JvmStatic
    fun entity(): LivingEntity? = frames.lastOrNull()?.entity

    @JvmStatic
    fun isMainArm(): Boolean {
        val frame = frames.lastOrNull() ?: return false
        return frame.arm == frame.entity.mainArm
    }
}
