package qouteall.imm_ptl.core.render.context_management;

/**
 * Compile-time stand-in for Immersive Portals 5.2.0's class of the same name. The real class lives in the
 * imm_ptl_core jar nested inside Immersive Portals, which the compiler cannot see. Not packed into our jar.
 */
public final class PortalRendering {
    private PortalRendering() {
    }

    public static boolean isRendering() {
        throw new UnsupportedOperationException("stub");
    }
}
