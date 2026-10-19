package com.atsuishio.superbwarfare.client

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.mojang.math.Axis
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import org.joml.Matrix4f
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 2D 纹理绘制与 GUI 辅助。
 * 
 * <h2>blit 的唯一锚点是 [PoseStack]</h2>
 * GUI 里传 `guiGraphics.pose()`，自绘节点里直接用当前 pose（例如 AUI 的
 * `BodyRenderNodeProvider#drawContentOnly(PoseStack)`），不再需要 [GuiGraphics]，
 * 也不再需要 `BufferSource`。
 * 
 * <h2>三种画法</h2>
 * 
 *  * [.blit]：源矩形与目标矩形同尺寸，只需要给 (u, v) 偏移。绝大多数场合用它。
 *  * [.blitRegion]：源矩形与目标矩形尺寸不同（拉伸 / 裁切）。
 * 
 * 颜色统一为 ARGB：`0xAARRGGBB`，alpha 参与混合
 */
object RenderHelper {
    /**
     * 把整张纹理（UV 0..1）拉伸到目标矩形 (x, y, width, height)。
     *
     * HUD 里绝大多数图标/边框都是这个形状，等价于
     * `blit(pose, texture, x, y, width, height, 0f, 0f, width, height, width, height, argb)`，
     * 但不用再传那 4 个恒定参数。要取纹理里的一块时用下面那个重载。
     *
     * @param argb 顶点颜色 `0xAARRGGBB`，默认不透明白色
     * @param z    顶点 z（旧 `blitOffset`），默认 0；item 装饰层需要 200 时给这里，别去改 pose
     */
    fun blit(
        pose: PoseStack, texture: ResourceLocation,
        x: Float, y: Float, width: Float, height: Float,
        argb: Int = -0x1, z: Float = 0f
    ) {
        blitRegionAt(pose, texture, x, y, width, height, 0f, 0f, width, height, width, height, z, argb)
    }

    /**
     * 把 `texture` 上 (u, v) 起、与目标矩形同尺寸的一块画到 (x, y, width, height)。
     * 
     * <pre>
     * blit(pose, atlas, x, y, w, h, u, v, 512, 512)       // 图集里的一块，图集 512x512
     * blit(pose, atlas, x, y, w, h, u = 128f, v = 0f, textureWidth = 512f, textureHeight = 16f)
    </pre>
     * 
     * @param pose          当前 PoseStack（GUI 里是 `guiGraphics.pose()`）
     * @param texture       纹理
     * @param x             目标左上角 X
     * @param y             目标左上角 Y
     * @param width         目标宽度，同时也是从纹理上截取出来的宽度
     * @param height        目标高度，同时也是从纹理上截取出来的高度
     * @param u             纹理内起点 X（单位为纹素）
     * @param v             纹理内起点 Y（单位为纹素）
     * @param textureWidth  整张纹理宽度，用于把 (u, v) 归一化成 UV
     * @param textureHeight 整张纹理高度
     * @param argb          顶点颜色 `0xAARRGGBB`，会和纹理采样结果相乘；alpha 参与混合
     * @param z             顶点 z（旧 `blitOffset`），默认 0
     */
    fun blit(
        pose: PoseStack, texture: ResourceLocation,
        x: Float, y: Float, width: Float, height: Float,
        u: Float, v: Float, textureWidth: Float, textureHeight: Float,
        argb: Int = -0x1, z: Float = 0f
    ) {
        blitRegionAt(pose, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight, z, argb)
    }

    /**
     * 把纹理上的源矩形 (u, v, uWidth, vHeight) 拉伸/裁切到目标矩形 (x, y, width, height)。
     * 
     * 
     * 只有源矩形和目标矩形尺寸不同时才需要它；同尺寸请用 [.blit]。
     * 
     * 
     * @param argb 顶点颜色 `0xAARRGGBB`，alpha 参与混合
     * @param z    顶点 z（旧 `blitOffset`），默认 0
     */
    fun blitRegion(
        pose: PoseStack, texture: ResourceLocation,
        x: Float, y: Float, width: Float, height: Float,
        u: Float, v: Float, uWidth: Float, vHeight: Float,
        textureWidth: Float, textureHeight: Float,
        argb: Int = -0x1, z: Float = 0f
    ) {
        blitRegionAt(pose, texture, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight, z, argb)
    }

    /**
     * 把 `0xRRGGBB` 补成不透明 `0xFFRRGGBB`；已经是 ARGB 的值原样返回。
     *
     * 专门给从旧 `preciseBlitWithColor` 迁过来的调用点用 —— 旧实现会强写 alpha，
     * 而 `0xFFFFFF` 这类 RGB 常量在新 API 里 alpha 是 0（完全透明）。确认过颜色本身
     * 就不透明之后，可以把 `opaque(...)` 去掉。
     */
    fun opaque(rgb: Int): Int = rgb or -0x1000000

    /**
     * 一张纹理 + 它的像素尺寸，可选带一个默认绘制区域。
     *
     * 声明一次当常量用，之后每次 blit 都不用再传 textureWidth/textureHeight，也不可能把图集尺寸写错。
     * 只填前三个参数就是整张；后面四个省略时有默认值：
     *
     *  * `u`/`v` 默认 0（左上角）
     *  * `width`/`height` 默认“从 u/v 一直取到贴图右下边界”，配合 u=-v=0 就是整张
     *
     * ```
     * // 整张
     * private val HUD_BASE = RenderHelper.Texture(loc("textures/gui/hud_base.png"), 320f, 320f)
     * // 只固定画其中一块：128x64 的贴图，从 (0, 6) 取 60x6
     * private val VALUE_BAR = RenderHelper.Texture(loc("textures/gui/hud_base.png"), 128f, 64f,
     *     u = 0f, v = 6f, width = 60f, height = 6f)
     *
     * RenderHelper.blit(pose, HUD_BASE, x - 160f, y - 160f)   // 整张，原尺寸
     * RenderHelper.blit(pose, VALUE_BAR, x, y)                // 默认区域，原尺寸
     * RenderHelper.blit(pose, VALUE_BAR, x, y, 120f, 12f)     // 同一块，放大一倍
     * RenderHelper.blit(pose, HUD_BASE.whole, x, y)           // 明确要整张
     * RenderHelper.blit(pose, HUD_BASE.sprite(0f, 6f, 60f, 6f), x, y)   // 用完就丢的一块
     * ```
     *
     * 只有区域固定的贴图才值得这样写；每帧变化的 uv（罗盘、能量条）继续用 float 版。
     */
    class Texture(
        val location: ResourceLocation,
        /** 整张贴图的像素尺寸 */
        val imageWidth: Float,
        val imageHeight: Float,
        /** 默认绘制区域（纹素），省略即整张 */
        val u: Float = 0f,
        val v: Float = 0f,
        val width: Float = imageWidth - u,
        val height: Float = imageHeight - v,
    ) {
        /** 整张纹理 */
        val whole: Sprite = Sprite(this, 0f, 0f, imageWidth, imageHeight)

        /** 这个 Texture 默认代表的那一块，[.blit] 画的就是它；零分配 */
        val region: Sprite = Sprite(this, u, v, width, height)

        /** 临时取纹理里的一块，单位是纹素 */
        fun sprite(u: Float, v: Float, width: Float, height: Float) = Sprite(this, u, v, width, height)

        /** 纹理里的一块；uv 一次性归一化好，绘制时不再做除法。 */
        class Sprite(
            val texture: Texture,
            val u: Float, val v: Float, val width: Float, val height: Float,
        ) {
            val u0: Float = u / texture.imageWidth
            val v0: Float = v / texture.imageHeight
            val u1: Float = (u + width) / texture.imageWidth
            val v1: Float = (v + height) / texture.imageHeight
        }
    }

    /**
     * 用 [Texture] 画它默认代表的那一块。目标尺寸默认等于区域尺寸，要缩放只给 width/height。
     */
    fun blit(
        pose: PoseStack, texture: Texture, x: Float, y: Float,
        width: Float = texture.width, height: Float = texture.height,
        argb: Int = -0x1, z: Float = 0f
    ) {
        blit(pose, texture.region, x, y, width, height, argb, z)
    }

    /**
     * 用 [Texture.Sprite] 画。目标尺寸默认等于源尺寸，要缩放只给 width/height。
     */
    fun blit(
        pose: PoseStack, sprite: Texture.Sprite, x: Float, y: Float,
        width: Float = sprite.width, height: Float = sprite.height,
        argb: Int = -0x1, z: Float = 0f
    ) {
        if (width == 0f || height == 0f) return
        emitQuad(
            pose.last().pose(), sprite.texture.location,
            x, y, x + width, y + height, z,
            sprite.u0, sprite.v0, sprite.u1, sprite.v1, argb
        )
    }

    private fun blitRegionAt(
        pose: PoseStack, texture: ResourceLocation,
        x: Float, y: Float, width: Float, height: Float,
        u: Float, v: Float, uWidth: Float, vHeight: Float,
        textureWidth: Float, textureHeight: Float,
        z: Float, argb: Int
    ) {
        // 空矩形直接跳过：HUD 里大量 0 高度/0 宽度（进度条、淡出中）的调用不该产生顶点。
        if (width == 0f || height == 0f || uWidth == 0f || vHeight == 0f) return
        emitQuad(
            pose.last().pose(), texture,
            x, y, x + width, y + height, z,
            u / textureWidth, v / textureHeight,
            (u + uWidth) / textureWidth, (v + vHeight) / textureHeight,
            argb
        )
    }

    // ==================================================================================
    //  版本相关：1.20 的 Tesselator / VertexConsumer API
    //  1.21 分支（sbw121）里这一段用 begin(mode, format) + addVertex/setUv/setColor/buildOrThrow()，
    //  并且用 POSITION_TEX_COLOR + getPositionTexColorShader。其余部分两边一致。
    // ==================================================================================
    /**
     * 立即画一个 POSITION_COLOR_TEX 四边形。
     * 
     * 
     * 这里只管顶点和着色器；混合函数由调用方负责（AUI 的 content node、GuiGraphics 的 HUD
     * 流程都会在调用前把 blend 打开并设好 blendFunc）。这里只保证 blend 是开着的，并且**绝不**
     * 在画完把它关掉 —— 旧 `innerBlit(color)` 结尾的 `disableBlend()` 会把同一批次里
     * 后续所有 untinted blit 的透明通道一起搞坏。
     */
    private fun emitQuad(
        matrix: Matrix4f, texture: ResourceLocation,
        x1: Float, y1: Float, x2: Float, y2: Float, z: Float,
        minU: Float, minV: Float, maxU: Float, maxV: Float,
        argb: Int
    ) {
        RenderSystem.setShaderTexture(0, texture)
        RenderSystem.setShader { GameRenderer.getPositionColorTexShader() }
        RenderSystem.enableBlend()

        val buffer = Tesselator.getInstance().builder
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX)
        buffer.vertex(matrix, x1, y1, z).color(argb).uv(minU, minV).endVertex()
        buffer.vertex(matrix, x1, y2, z).color(argb).uv(minU, maxV).endVertex()
        buffer.vertex(matrix, x2, y2, z).color(argb).uv(maxU, maxV).endVertex()
        buffer.vertex(matrix, x2, y1, z).color(argb).uv(maxU, minV).endVertex()
        BufferUploader.drawWithShader(buffer.end())
    }

    // ==================================================================================
    //  GUI 专用辅助（需要 GuiGraphics / BufferSource，与 blit 无关）
    // ==================================================================================
    /**
     * Fills a rectangle with the specified color and z-level using the given render type and coordinates as the boundaries.
     * 
     * @param renderType the render type to use.
     * @param minX       the minimum x-coordinate of the rectangle.
     * @param minY       the minimum y-coordinate of the rectangle.
     * @param maxX       the maximum x-coordinate of the rectangle.
     * @param maxY       the maximum y-coordinate of the rectangle.
     * @param z          the z-level of the rectangle.
     * @param color      the color to fill the rectangle with.
     */
    fun fill(
        guiGraphics: GuiGraphics,
        renderType: RenderType,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float,
        z: Float,
        color: Int
    ) {
        var minX = minX
        var minY = minY
        var maxX = maxX
        var maxY = maxY
        val matrix4f = guiGraphics.pose().last().pose()
        if (minX < maxX) {
            val i = minX
            minX = maxX
            maxX = i
        }

        if (minY < maxY) {
            val j = minY
            minY = maxY
            maxY = j
        }

        val vertexconsumer = guiGraphics.bufferSource().getBuffer(renderType)
        vertexconsumer.vertex(matrix4f, minX, minY, z).color(color).endVertex()
        vertexconsumer.vertex(matrix4f, minX, maxY, z).color(color).endVertex()
        vertexconsumer.vertex(matrix4f, maxX, maxY, z).color(color).endVertex()
        vertexconsumer.vertex(matrix4f, maxX, minY, z).color(color).endVertex()

        guiGraphics.flush()
    }

    fun renderScrollingString(
        pGuiGraphics: GuiGraphics,
        pFont: Font,
        pText: Component,
        scale: Float,
        pMinX: Int,
        pMinY: Int,
        pMaxX: Int,
        pMaxY: Int,
        pColor: Int
    ) {
        val width = pFont.width(pText)
        val borderWidth = pMaxX - pMinX
        if (width > borderWidth) {
            val l = width - borderWidth
            val rate = Util.getMillis().toDouble() / 1000
            val d1 = max(l.toDouble() * 0.5, 3.0)
            val d2 = sin((Math.PI / 2) * cos((Math.PI * 2) * rate / d1)) / 2 + 0.5
            val d3 = Mth.lerp(d2, 0.0, l.toDouble())
            pGuiGraphics.enableScissor(
                (pMinX * scale).toInt(),
                (pMinY * scale).toInt(),
                (pMaxX * scale).toInt(),
                (pMaxY * scale).toInt()
            )
            pGuiGraphics.drawString(pFont, pText, pMinX - d3.toInt(), pMinY, pColor)
            pGuiGraphics.disableScissor()
        } else {
            pGuiGraphics.drawString(pFont, pText, pMinX, pMinY, pColor)
        }
    }

    fun renderCenteredScrollingString(
        pGuiGraphics: GuiGraphics,
        pFont: Font,
        pText: Component,
        scale: Float,
        pMinX: Int,
        pMinY: Int,
        pMaxX: Int,
        pMaxY: Int,
        pColor: Int
    ) {
        val width = pFont.width(pText)
        val borderWidth = pMaxX - pMinX
        if (width > borderWidth) {
            val l = width - borderWidth
            val rate = Util.getMillis().toDouble() / 1000
            val d1 = max(l.toDouble() * 0.5, 3.0)
            val d2 = sin((Math.PI / 2) * cos((Math.PI * 2) * rate / d1)) / 2 + 0.5
            val d3 = Mth.lerp(d2, 0.0, l.toDouble())
            pGuiGraphics.enableScissor(
                (pMinX * scale).toInt(),
                (pMinY * scale).toInt(),
                (pMaxX * scale).toInt(),
                (pMaxY * scale).toInt()
            )
            pGuiGraphics.drawString(pFont, pText, pMinX - d3.toInt(), pMinY, pColor)
            pGuiGraphics.disableScissor()
        } else {
            pGuiGraphics.drawCenteredString(pFont, pText, (pMinX + pMaxX) / 2, pMinY, pColor)
        }
    }

    /**
     * 渲染一个圆环
     * 
     * @param guiGraphics     gui
     * @param centerX         渲染中心X坐标
     * @param centerY         渲染中心Y坐标
     * @param outerRadius     外环半径
     * @param innerRadius     内环半径
     * @param backgroundColor 背景颜色
     * @param progressColor   进度颜色
     * @param progress        进度
     * @param useRate         是否使用占据屏幕百分比形式的半径
     */
    fun renderCircularRing(
        guiGraphics: GuiGraphics,
        centerX: Float,
        centerY: Float,
        outerRadius: Float,
        innerRadius: Float,
        backgroundColor: FloatArray,
        progressColor: FloatArray,
        progress: Float,
        useRate: Boolean
    ) {
        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader { GameRenderer.getPositionColorShader() }

        poseStack.rotateAround(Axis.ZP.rotationDegrees(-90f), centerX, centerY, 0f)

        val window = Minecraft.getInstance().window
        val scale = (if (useRate) min(window.guiScaledWidth, window.guiScaledHeight) else 1).toFloat()

        // 绘制背景圆环
        drawCircularRing(guiGraphics, centerX, centerY, outerRadius * scale, innerRadius * scale, backgroundColor, 1.0f)

        // 绘制进度圆环
        drawCircularRing(
            guiGraphics,
            centerX,
            centerY,
            outerRadius * scale,
            innerRadius * scale,
            progressColor,
            progress
        )

        poseStack.popPose()
        RenderSystem.disableBlend()
    }

    fun drawCircularRing(
        guiGraphics: GuiGraphics,
        centerX: Float,
        centerY: Float,
        outerRadius: Float,
        innerRadius: Float,
        color: FloatArray,
        progressAngle: Float
    ) {
        val buffer = Tesselator.getInstance().builder
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)

        val pose = guiGraphics.pose().last()
        val matrix = pose.pose()
        val angleStep = (2 * Math.PI / 180).toFloat()
        val maxAngle = (2 * Math.PI * progressAngle).toFloat()

        var i = 0
        while (i <= 180 * progressAngle) {
            var angle = i * angleStep
            if (angle > maxAngle) {
                angle = maxAngle
            }

            val cos = cos(angle.toDouble()).toFloat()
            val sin = sin(angle.toDouble()).toFloat()

            // 外圆点
            val outerX = centerX + outerRadius * cos
            val outerY = centerY + outerRadius * sin
            buffer.vertex(matrix, outerX, outerY, 0f)
                .color(color[0], color[1], color[2], color[3])
                .endVertex()

            // 内圆点
            val innerX = centerX + innerRadius * cos
            val innerY = centerY + innerRadius * sin
            buffer.vertex(matrix, innerX, innerY, 0f)
                .color(color[0], color[1], color[2], color[3])
                .endVertex()

            if (angle >= maxAngle) break
            i++
        }

        BufferUploader.drawWithShader(buffer.end())
    }
}
