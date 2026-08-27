package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.model.GunModelReloadListener
import com.atsuishio.superbwarfare.tools.localPlayer
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.maydaymemory.mae.basic.DummyPose
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.control.runner.AnimationContext
import com.maydaymemory.mae.control.runner.AnimationRunner
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf

open class GeoGunAnimationInstance(
    private var stack: ItemStack,
    entity: Entity,
    hand: InteractionHand
) : IFPAnimationInstance {
    private val animations = hashMapOf<String, BedrockAnimation>()
    private var runner: AnimationRunner? = null
    private var currentState: GunAnimationState? = null
    private var cachedPose: Pose = DummyPose.INSTANCE
    private val cameraRotation = Quaternionf()

    init {
        loadAnimations()
    }

    private fun loadAnimations() {
        val location = GunResource.compute(stack).getModel().animation ?: return
        GunModelReloadListener.getAnimation(location)?.forEach { animation ->
            animations[animation.name] = animation
        }
    }

    private fun resolveState(): GunAnimationState? {
        val player = localPlayer ?: return null
        val animation = GunResource.compute(stack).animation ?: return null
        val data = GunData.from(stack)

        if (animation.edit != null && ClientEventHandler.isEditing) return GunAnimationState.EDIT
        if (animation.bolt != null && data.bolt.actionTimer.get() > 0) return GunAnimationState.BOLT

        if (data.reloading()) {
            if (animation.reload != null) return GunAnimationState.RELOAD
            if (animation.reloadNormal != null && data.reload.normal()) return GunAnimationState.RELOAD_NORMAL
            if (animation.reloadEmpty != null && data.reload.empty()) return GunAnimationState.RELOAD_EMPTY
        }

        if (animation.melee != null && ClientEventHandler.gunMelee > 0) return GunAnimationState.MELEE
        if (animation.fire != null && ClientEventHandler.holdingFireKey && data.canShoot(player)) {
            return GunAnimationState.FIRE
        }

        if (animation.run != null
            && player.isSprinting
            && player.onGround()
            && ClientEventHandler.noSprintTicks == 0f
            && ClientEventHandler.drawTime < 0.01
        ) {
            return GunAnimationState.RUN
        }

        return if (animation.idle != null) GunAnimationState.IDLE else null
    }

    private fun animationName(state: GunAnimationState): String? {
        val animation = GunResource.compute(stack).animation ?: return null
        return when (state) {
            GunAnimationState.IDLE -> animation.idle
            GunAnimationState.EDIT -> animation.edit
            GunAnimationState.BOLT -> animation.bolt
            GunAnimationState.RELOAD -> animation.reload
            GunAnimationState.RELOAD_NORMAL -> animation.reloadNormal
            GunAnimationState.RELOAD_EMPTY -> animation.reloadEmpty
            GunAnimationState.MELEE -> animation.melee
            GunAnimationState.FIRE -> animation.fire
            GunAnimationState.RUN -> animation.run
        }
    }

    private fun play(state: GunAnimationState) {
        val name = animationName(state) ?: return
        val animation = animations[name] ?: return
        val newRunner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
        newRunner.state = state.playType.state()
        runner = newRunner
        currentState = state
        cachedPose = newRunner.evaluate()
    }

    override fun currentItem(): ItemStack = stack

    override fun getPose(): Pose = cachedPose

    override fun getCachedPose(): Pose = cachedPose

    override fun tick(partialTicks: Float) {
        val target = resolveState()
        if (target == null) {
            runner = null
            currentState = null
            cachedPose = DummyPose.INSTANCE
            return
        }

        if (runner == null || currentState != target) {
            play(target)
        } else {
            runner?.tick()
        }

        cachedPose = runner?.evaluate() ?: DummyPose.INSTANCE
    }

    override fun getCameraRotation(): Quaternionf = cameraRotation

    override fun setCameraRotation(rotation: Quaternionf) {
        cameraRotation.set(rotation)
    }

    override fun updateItem(stack: ItemStack) {
        this.stack = stack
    }

    override fun triggerDraw() {
        if (runner == null) {
            play(resolveState() ?: GunAnimationState.IDLE)
        }
    }

    override fun triggerPutAway() {
        runner = null
        currentState = null
        cachedPose = DummyPose.INSTANCE
    }

    override fun shouldRenderHand(): Boolean {
        return true
    }
}
