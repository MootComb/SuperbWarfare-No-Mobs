package com.atsuishio.superbwarfare.client.overlay.ammo

import com.atsuishio.superbwarfare.client.RenderHelper
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.sighs.apricityui.init.Document
import com.sighs.apricityui.init.Element
import com.sighs.apricityui.render.*
import com.sighs.apricityui.spi.AuiItemRenderRequest
import com.sighs.apricityui.spi.AuiServices
import com.sighs.apricityui.style.Filter
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LightTexture
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class AmmoHudResourceGroup(document: Document) : Element(document, TAG), BodyRenderNodeProvider {
    internal data class Group(val resources: List<AmmoHudResources.Resource>, val x: Double, val opacity: Double)
    private data class Scene(val groups: List<Group>, val frames: List<AmmoHudFrames.Pose>)
    @Volatile private var scene = Scene(emptyList(), emptyList())

    internal fun show(groups: List<Group>, frames: List<AmmoHudFrames.Pose>) {
        scene = Scene(groups, frames)
    }

    fun clear() {
        scene = Scene(emptyList(), emptyList())
    }

    override fun createBodyRenderNodes(): List<RenderNode> = listOf(RenderNode.ElementContentNode(this))

    override fun drawContentOnly(pose: PoseStack) {
        val current = scene
        if (current.groups.isEmpty() && current.frames.isEmpty()) return
        Base.commitLocalDraws()
        val position = Rect.of(this).contentPosition
        pose.pushPose()
        try {
            pose.translate(position.x, position.y, 0.0)
            for (corner in current.frames) {
                if (corner.opacity < 0.004) continue
                pose.pushPose()
                try {
                    pose.translate(corner.x, 0.0, 0.0)
                    layer(corner.opacity.toFloat()) {
                        frame(pose)
                        if (corner.index > 0) {
                            val plusWidth = Minecraft.getInstance().font.width("+") * 0.6
                            label(pose, "+", (-(STRIDE - SIZE + plusWidth) / 2).toFloat(), 3.3f, 0xffcccccc.toInt())
                        }
                    }
                } finally {
                    pose.popPose()
                }
            }
            for (group in current.groups) {
                if (group.resources.isEmpty() || group.opacity < 0.004) continue
                pose.pushPose()
                try {
                    pose.translate(group.x, 0.0, 0.0)
                    // Keep native item depth/compositing identical at alpha 1 and during motion.
                    // Avoid changing the render path just before the slide settles at alpha 1.
                    layer(group.opacity.toFloat(), force = true) {
                        for ((index, resource) in group.resources.withIndex()) {
                            pose.pushPose()
                            try {
                                pose.translate(index * STRIDE, 0.0, 0.0)
                                layer(if (resource.low) 0.5f else 1f, if (resource.low) LOW else Filter.FilterState.EMPTY) {
                                    pose.pushPose()
                                    try {
                                        // The 12px frame and actual 9.6px model share the texture scale.
                                        pose.translate(1.2, 1.2, 0.0)
                                        pose.scale(0.6f, 0.6f, 1f)
                                        when (val icon = resource.icon) {
                                            is AmmoHudResources.Icon.Item -> AuiServices.items().render(
                                                AuiItemRenderRequest(pose, icon.stack, 0, false, null, 0f, false)
                                            )
                                            is AmmoHudResources.Icon.Texture -> RenderHelper.blit(
                                                pose, icon.location, 0f, 0f, 16f, 16f
                                            )
                                        }
                                    } finally {
                                        pose.popPose()
                                    }
                                }
                                if (resource.low) label(pose, "!", 9f, -1f, 0xffff5555.toInt())
                            } finally {
                                pose.popPose()
                            }
                        }
                    }
                } finally {
                    pose.popPose()
                }
            }
        } finally {
            pose.popPose()
        }
    }

    // 完整清空复用缓冲，不能只清理局部裁剪区，否则会残留上一帧图标。
    private inline fun layer(
        opacity: Float, filter: Filter.FilterState = Filter.FilterState.EMPTY,
        force: Boolean = false, draw: () -> Unit
    ) {
        if (!force && opacity >= 0.999f && filter.isEmpty) {
            draw()
            return
        }
        Base.commitLocalDraws()
        val clip = Mask.getCurrentScissor()
        RenderSystem.disableScissor()
        try {
            FilterRenderer.pushFilter()
        } finally {
            Mask.restoreScissor(clip)
        }
        try {
            draw()
        } finally {
            Base.commitLocalDraws()
            FilterRenderer.popFilter(filter, opacity)
        }
    }

    private fun label(pose: PoseStack, text: String, x: Float, y: Float, color: Int) {
        val mc = Minecraft.getInstance()
        pose.pushPose()
        try {
            pose.translate(x.toDouble(), y.toDouble(), Base.getGuiItemForegroundZ().toDouble())
            pose.scale(0.6f, 0.6f, 1f)
            val buffers = mc.renderBuffers().bufferSource()
            mc.font.drawInBatch(text, 0f, 0f, color, true, pose.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT)
            buffers.endBatch()
        } finally {
            pose.popPose()
        }
    }

    private fun frame(pose: PoseStack) {
        RenderSystem.enableBlend()
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        val buffer = Tesselator.getInstance().builder
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        fun rect(x: Float, y: Float, w: Float, h: Float) {
            val matrix = pose.last().pose()
            buffer.vertex(matrix, x, y, 0f).color(255, 255, 255, 255).endVertex()
            buffer.vertex(matrix, x, y + h, 0f).color(255, 255, 255, 255).endVertex()
            buffer.vertex(matrix, x + w, y + h, 0f).color(255, 255, 255, 255).endVertex()
            buffer.vertex(matrix, x + w, y, 0f).color(255, 255, 255, 255).endVertex()
        }
        for (x in listOf(0f, 10f)) for (y in listOf(0f, 10f)) {
            rect(x, if (y == 0f) 0f else 11f, 2f, 1f)
            rect(if (x == 0f) 0f else 11f, if (y == 0f) 1f else 10f, 1f, 1f)
        }
        BufferUploader.drawWithShader(buffer.end())
    }

    companion object {
        private const val TAG = "SBW-HUD-RESOURCES"
        private const val SIZE = AmmoHudFrames.SIZE
        private const val STRIDE = AmmoHudFrames.STRIDE
        private val LOW = Filter.parse("sepia(1) hue-rotate(300deg)", 1f)
        private var registered = false

        fun register() {
            if (registered) return
            register(TAG) { document, _ -> AmmoHudResourceGroup(document) }
            registered = true
        }
    }
}
