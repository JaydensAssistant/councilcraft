package dev.councilcraft.compat.client;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

/**
 * PPM7. With Iris, when the camera is within about 0.1 block of a portal plane, Immersive Portals pastes
 * the portal view over the first-person hand that was already drawn. Right after IP initialises the stencil
 * for the outer layer, this marks every pixel the hand covers (depth nearer than the hand's far depth) with
 * a stencil value IP's portal pass will not draw over. Client only; outer world only, not inside a portal view.
 */
public final class PortalHandMask {
    /** Iris draws the hand with its depth range squeezed to [0, 0.5625]; anything nearer is the hand. */
    static final double HAND_FAR_DEPTH = 0.5625;
    static final int MASK = 0xFF;

    private PortalHandMask() {
    }

    public static void markHand() {
        if (PortalRendering.isRendering()) {
            return;
        }
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean stencilTest = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int stencilWriteMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);

        // A full-screen triangle at the hand's far depth passes only where something nearer (the hand) is drawn.
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_GREATER);
        GlStateManager._depthMask(false);
        GlStateManager._colorMask(false, false, false, false);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(MASK);
        GL11.glStencilFunc(GL11.GL_ALWAYS, MASK, MASK);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glDepthRange(HAND_FAR_DEPTH, HAND_FAR_DEPTH);
        MyRenderHelper.renderScreenTriangle();
        GL11.glDepthRange(0.0, 1.0);

        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._depthMask(depthMask);
        GlStateManager._depthFunc(depthFunc);
        if (!depthTest) {
            GlStateManager._disableDepthTest();
        }
        GL11.glStencilMask(stencilWriteMask);
        if (!stencilTest) {
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }
    }
}
