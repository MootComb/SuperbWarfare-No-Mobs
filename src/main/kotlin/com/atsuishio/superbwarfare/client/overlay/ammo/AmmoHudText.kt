package com.atsuishio.superbwarfare.client.overlay.ammo

import com.mojang.blaze3d.vertex.PoseStack
import com.sighs.apricityui.init.Document
import com.sighs.apricityui.init.Element
import com.sighs.apricityui.layout.Box
import com.sighs.apricityui.render.Base
import com.sighs.apricityui.render.BodyRenderNodeProvider
import com.sighs.apricityui.render.Rect
import com.sighs.apricityui.render.RenderNode
import com.sighs.apricityui.style.Text
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class AmmoHudText(document: Document) : Element(document, TAG), BodyRenderNodeProvider {
    private var label = ""
    private var fontScale = 1f
    private var measuredFont: Font? = null

    override fun onInitFromDom(origin: Element) {
        fontScale = getAttribute("data-scale")?.toFloatOrNull()?.coerceAtLeast(0.1f) ?: 1f
        setLabel(getAttribute("data-label") ?: "")
    }

    fun setLabel(value: String) {
        val font = Minecraft.getInstance().font
        if (label == value && measuredFont === font) return
        label = value
        measuredFont = font
        // Keep the DOM text empty: AUI must neither rasterize nor select an imitation font.
        if (getAttribute("data-fixed-width") != "true") {
            setInlineStyleProperty("width", "${font.width(label) * fontScale}px")
        }
        setInlineStyleProperty("height", "${font.lineHeight * fontScale}px")
    }

    override fun createBodyRenderNodes(): List<RenderNode> = listOf(RenderNode.ElementContentNode(this))

    override fun drawContentOnly(pose: PoseStack) {
        if (label.isEmpty()) return
        val mc = Minecraft.getInstance()
        val font = mc.font
        val color = Text.getFontColor(this)
        if (color ushr 24 < 4) return
        val position = Rect.of(this).contentPosition
        val width = Box.of(this).innerSize().width()
        val textWidth = font.width(label) * fontScale
        val offset = when (getAttribute("data-align")) {
            "center" -> (width - textWidth) / 2
            "right" -> width - textWidth
            else -> 0.0
        }
        // Flush while AUI's current clip/compositing state is active, preserving ordering with its icons.
        Base.commitLocalDraws()
        pose.pushPose()
        try {
            pose.translate(position.x + offset, position.y, 0.0)
            pose.scale(fontScale, fontScale, 1f)
            val buffers = mc.renderBuffers().bufferSource()
            font.drawInBatch(label, 0f, 0f, color, true, pose.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT)
            buffers.endBatch()
        } finally {
            pose.popPose()
        }
    }

    companion object {
        private const val TAG = "SBW-HUD-TEXT"
        private var registered = false

        fun register() {
            if (registered) return
            register(TAG) { document, _ -> AmmoHudText(document) }
            registered = true
        }
    }
}
