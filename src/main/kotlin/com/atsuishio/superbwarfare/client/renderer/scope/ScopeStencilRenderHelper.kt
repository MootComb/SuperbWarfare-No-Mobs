package com.atsuishio.superbwarfare.client.renderer.scope

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30

@OnlyIn(Dist.CLIENT)
object ScopeStencilRenderHelper {

    /** GL_STENCIL_BITS is not a valid glGet query in the core profile used by Minecraft. */
    @JvmStatic
    fun hasStencilAttachment(): Boolean {
        if (GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) == 0) return false
        val type = GL30.glGetFramebufferAttachmentParameteri(
            GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT,
            GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        )
        return type != GL11.GL_NONE && GL30.glGetFramebufferAttachmentParameteri(
            GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT,
            GL30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE
        ) > 0
    }

    @JvmStatic
    fun enableItemEntityStencilTest() {
        RenderSystem.assertOnRenderThread()
        Minecraft.getInstance().mainRenderTarget.enableStencil()
        GL11.glEnable(GL11.GL_STENCIL_TEST)
    }

    @JvmStatic
    fun disableItemEntityStencilTest() {
        RenderSystem.assertOnRenderThread()
        GL11.glDisable(GL11.GL_STENCIL_TEST)
    }
}
