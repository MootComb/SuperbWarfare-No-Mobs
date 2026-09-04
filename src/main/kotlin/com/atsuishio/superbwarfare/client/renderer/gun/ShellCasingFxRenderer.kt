package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.model.shell.BedrockShellModel
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.gun.pojo.ShellEjectInfo
import com.atsuishio.superbwarfare.resource.model.ShellModelReloadListener
import com.atsuishio.superbwarfare.tools.mulPoseMatrix
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.*

object ShellCasingFxRenderer {
    private const val DEFAULT_SHELL_BONE = "shell"

    private val queue = ArrayDeque<ShellCasingInstance>()

    fun render(
        poseStack: PoseStack,
        model: GeoGunModel,
        stack: ItemStack,
        hand: InteractionHand,
        bufferSource: MultiBufferSource,
        packedLight: Int
    ) {
        val config = GunResource.compute(stack).shellEject
        val shellModelLocation = config?.shellModel
        val shellTexture = config?.shellTexture
        val animation = FirstPersonRenderHandler.getActiveAnimationInstance(hand) as? GeoGunAnimationInstance
        if (config == null || shellModelLocation == null || shellTexture == null) {
            animation?.consumePendingShellEjects()
            queue.clear()
            return
        }

        val shellModel = ShellModelReloadListener.getModel(shellModelLocation) ?: run {
            animation?.consumePendingShellEjects()
            queue.clear()
            return
        }

        if (animation != null) {
            for (window in animation.consumePendingShellEjects()) {
                spawn(poseStack, model, config, window)
            }
        }

        renderActive(bufferSource, shellModel, shellTexture, config, packedLight)
    }

    private fun spawn(poseStack: PoseStack, model: GeoGunModel, config: ShellEjectInfo, window: Int) {
        val baseBone = config.boneName.ifBlank { DEFAULT_SHELL_BONE }
        val boneName = if (window <= 0) baseBone else "${baseBone}_$window"
        val boneTransform = model.getGlobalTransform(boneName) ?: return

        val randomOffset = Vector3f(
            ((Math.random() - 0.5) * 2 * config.randomVelocity.x).toFloat(),
            ((Math.random() - 0.5) * 2 * config.randomVelocity.y).toFloat(),
            ((Math.random() - 0.5) * 2 * config.randomVelocity.z).toFloat()
        )
        val randomAngle = Vector3f(
            ((Math.random() - 0.5) * 2 * config.randomAngle.x).toFloat(),
            ((Math.random() - 0.5) * 2 * config.randomAngle.y).toFloat(),
            ((Math.random() - 0.5) * 2 * config.randomAngle.z).toFloat()
        )
        val frozenTransform = Matrix4f(poseStack.last().pose()).mul(boneTransform)
        queue.offerLast(ShellCasingInstance(System.currentTimeMillis(), randomOffset, randomAngle, frozenTransform))

        val maxActive = config.maxActive.coerceAtLeast(1)
        while (queue.size > maxActive) {
            queue.pollFirst()
        }
    }

    private fun renderActive(
        bufferSource: MultiBufferSource,
        shellModel: BedrockShellModel,
        shellTexture: ResourceLocation,
        config: ShellEjectInfo,
        packedLight: Int
    ) {
        val lifeMs = (config.livingTime * 1000.0).toLong()
        val now = System.currentTimeMillis()

        while (queue.isNotEmpty() && now - queue.peekFirst().timestamp > lifeMs) {
            queue.pollFirst()
        }
        if (queue.isEmpty()) return

        val quadType = RenderType.entityCutout(shellTexture)
        val triangleType = BedrockModelRenderTypes.polyMeshCutout(shellTexture)
        for (shell in queue) {
            renderSingle(bufferSource, shellModel, quadType, triangleType, config, shell, now, packedLight)
        }
    }

    private fun renderSingle(
        bufferSource: MultiBufferSource,
        shellModel: BedrockShellModel,
        quadType: RenderType,
        triangleType: RenderType,
        config: ShellEjectInfo,
        shell: ShellCasingInstance,
        now: Long,
        packedLight: Int
    ) {
        val t = (now - shell.timestamp) / 1000.0
        val x = (config.initialVelocity.x + shell.randomOffset.x) * t + 0.5 * config.acceleration.x * t * t
        val y = (config.initialVelocity.y + shell.randomOffset.y) * t + 0.5 * config.acceleration.y * t * t
        val z = (config.initialVelocity.z + shell.randomOffset.z) * t + 0.5 * config.acceleration.z * t * t

        val poseStack = PoseStack()
        poseStack.mulPoseMatrix(shell.frozenTransform)
        poseStack.scale(config.size.coerceAtLeast(0.01f), config.size.coerceAtLeast(0.01f), config.size.coerceAtLeast(0.01f))
        poseStack.translate(x.toFloat(), y.toFloat(), (-z).toFloat())
        poseStack.mulPose(Axis.XN.rotationDegrees(shell.randomAngle.x + (config.angularVelocity.x * t).toFloat()))
        poseStack.mulPose(Axis.YN.rotationDegrees(shell.randomAngle.y + (config.angularVelocity.y * t).toFloat()))
        poseStack.mulPose(Axis.ZP.rotationDegrees(shell.randomAngle.z + (config.angularVelocity.z * t).toFloat()))

        shellModel.renderToBuffer(
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            packedLight,
            OverlayTexture.NO_OVERLAY
        )
    }

    private class ShellCasingInstance(
        val timestamp: Long,
        val randomOffset: Vector3f,
        val randomAngle: Vector3f,
        val frozenTransform: Matrix4f
    )
}
