package dev.councilcraft.compat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

/**
 * PPM5. Inside an Immersive Portals portal view, Iris' Distant Horizons depth texture still holds the
 * outer world's LOD depth, so ghost terrain shows through the portal. While IP renders a portal view,
 * {@code IrisDhPortalDepthMixin} hands Iris this all-far (1.0) depth texture instead. Client only; loaded
 * only when Iris, Distant Horizons and Immersive Portals are all present.
 */
public final class PortalDhDepth {
    private static int texture;
    private static int width;
    private static int height;

    private PortalDhDepth() {
    }

    public static boolean inPortalView() {
        return PortalRendering.isRendering();
    }

    /** A depth texture the size of the main framebuffer, cleared to far. Recreated when the window resizes. */
    public static int emptyDepthTexture() {
        RenderSystem.assertOnRenderThread();
        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        int w = Math.max(1, main.textureWidth);
        int h = Math.max(1, main.textureHeight);
        if (texture == 0 || w != width || h != height) {
            upload(w, h);
        }
        return texture;
    }

    private static void upload(int w, int h) {
        // Save the GL state we touch, so Iris and Sodium see it unchanged.
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int previousUnpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int alignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        if (texture == 0) {
            texture = GL11.glGenTextures();
        }
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
        FloatBuffer ones = MemoryUtil.memAllocFloat(w * h);
        try {
            for (int i = 0; i < w * h; i++) {
                ones.put(i, 1.0f);
            }
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, w, h, 0,
                GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, ones);
        } finally {
            MemoryUtil.memFree(ones);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, previousUnpackBuffer);
        width = w;
        height = h;
    }
}
