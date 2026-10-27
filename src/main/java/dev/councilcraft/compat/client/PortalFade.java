package dev.councilcraft.compat.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * PT45 (amends PT31). Fog through portals, no snap on crossing, and a 3 s ease-in clear.
 * <ul>
 * <li>Shaders on, portal view: while Immersive Portals renders a portal view, the Iris dhRenderDistance supplier
 * returns V = {@link #vanillaEdge()} (the real-chunk edge), so the destination's own shader fog fades the portal-view
 * terrain into that dimension's fog color. Shader packs are not modified.</li>
 * <li>Crossing: the fade starts where the portal view's fog was on the last frame. Shaders on that is V. Shaders off it
 * is vanilla's terrain fog start and end ({@link #vanillaFog}), converted to Distant Horizons far-fog fractions in
 * {@code PortalFadeDhFog}. Everything is read live from the player's current settings.</li>
 * <li>Clear: T = {@link #CLEAR_SECONDS} from the crossing, no hold. e = {@link #easeAt}, a cubic ease-in with a small
 * linear part so it moves from the first frame; d = V x (L / V)^e on the shaders-on path (log scale).</li>
 * </ul>
 * Client only; loaded only when Distant Horizons and Immersive Portals are present.
 */
public final class PortalFade {
    /** PT45: the clear takes 3.0 s. */
    static final double CLEAR_SECONDS = 3.0;
    /** Share of the ease that is linear, so the clear visibly moves from the first frame. */
    static final double EASE_LINEAR = 0.2;
    /** Immersive Portals' default indirectLoadingRadiusCap (chunks), used when IPGlobal can't be read. */
    static final int IP_CAP_DEFAULT = 8;

    private static long startNanos;
    private static boolean active;
    private static float startFogStart;
    private static float startFogEnd;
    private static RegistryKey<World> lastDimension;

    private PortalFade() {
    }

    /** Starts the fade. Called at the end of IP's ClientTeleportationManager.changePlayerDimension. */
    public static void begin() {
        MinecraftClient client = MinecraftClient.getInstance();
        lastDimension = client.world == null ? null : client.world.getRegistryKey();
        start(lastDimension);
    }

    /** End of every client tick: catches dimension changes that do not go through Immersive Portals. */
    public static void onEndTick(MinecraftClient client) {
        RegistryKey<World> dimension = client.world == null ? null : client.world.getRegistryKey();
        if (dimension == lastDimension) {
            return;
        }
        boolean changed = dimension != null && lastDimension != null;
        lastDimension = dimension;
        if (changed) {
            start(dimension);
        } else {
            // Joining or leaving a world is not a crossing.
            active = false;
        }
    }

    private static void start(RegistryKey<World> destination) {
        startNanos = System.nanoTime();
        active = destination != null;
        float[] fog = vanillaFog(MinecraftClient.getInstance());
        startFogStart = fog[0];
        startFogEnd = fog[1];
    }

    /** Eased clear progress e (0 at the crossing, 1 when done); 1 outside a fade. */
    public static float progress() {
        if (!active) {
            return 1.0F;
        }
        double e = easeAt((System.nanoTime() - startNanos) / 1.0E9);
        if (e >= 1.0) {
            active = false;
        }
        return (float) e;
    }

    /** 0..1 for shader packs (uniform cc_portalFade): 1 at the crossing, 0 when clear. */
    public static float fade() {
        return 1.0F - progress();
    }

    /** True while a fade is running and the current frame is not a portal view. */
    public static boolean fading() {
        return active && !PortalDhDepth.inPortalView();
    }

    /** Vanilla terrain fog start (blocks) at the crossing, from the live settings of the destination dimension. */
    public static float startFogStart() {
        return startFogStart;
    }

    /** Vanilla terrain fog end (blocks) at the crossing. */
    public static float startFogEnd() {
        return startFogEnd;
    }

    /**
     * The dhRenderDistance (blocks) shaders should see. In a portal view it is V, so the destination's fog closes in
     * at the real-chunk edge. Otherwise, during the fade, it recedes from V to the real value. Never above the real
     * value.
     */
    public static int effectiveDhRenderDistance(int real) {
        if (PortalDhDepth.inPortalView()) {
            return portalViewDistance(vanillaEdge(), real);
        }
        if (!active) {
            return real;
        }
        float e = progress();
        if (e >= 1.0F) {
            return real;
        }
        return distance(vanillaEdge(), real, e);
    }

    /** V: the real-chunk edge seen through the portal, in blocks. */
    public static int vanillaEdge() {
        int chunks = MinecraftClient.getInstance().options.getClampedViewDistance();
        return 16 * Math.min(chunks, ipCap());
    }

    private static int ipCap() {
        try {
            Class<?> global = Class.forName("qouteall.imm_ptl.core.IPGlobal");
            int cap = global.getField("indirectLoadingRadiusCap").getInt(null);
            return cap > 0 ? cap : IP_CAP_DEFAULT;
        } catch (ReflectiveOperationException | LinkageError e) {
            return IP_CAP_DEFAULT;
        }
    }

    /**
     * Vanilla's terrain fog (BackgroundRenderer.applyFog, 1.20.1) as {start, end} in blocks. Thick-fog dimensions
     * (the Nether) use 5% of the view distance to half of min(view distance, 192); everything else ends at the view
     * distance and starts clamp(vd / 10, 4, 64) blocks before it.
     */
    static float[] vanillaFog(MinecraftClient client) {
        float viewDistance = client.options.getClampedViewDistance() * 16.0F;
        boolean thick = false;
        if (client.world != null && client.player != null) {
            thick = client.world.getDimensionEffects().useThickFog(MathHelper.floor(client.player.getX()),
                MathHelper.floor(client.player.getZ()));
        }
        return vanillaFog(viewDistance, thick);
    }

    static float[] vanillaFog(float viewDistance, boolean thick) {
        if (thick) {
            return new float[] {viewDistance * 0.05F, Math.min(viewDistance, 192.0F) * 0.5F};
        }
        return new float[] {viewDistance - MathHelper.clamp(viewDistance / 10.0F, 4.0F, 64.0F), viewDistance};
    }

    /**
     * Vanilla fog start and end (blocks) as fractions of the LOD distance, {start, end}. Null when the fog is not
     * inside the LOD range, so there is nothing to fade.
     */
    public static float[] fogFractions(float startBlocks, float endBlocks, int lod) {
        if (lod <= 0 || endBlocks <= 0 || startBlocks >= lod) {
            return null;
        }
        float start = Math.max(0.0F, startBlocks / lod);
        float end = Math.min(1.0F, endBlocks / lod);
        return end > start ? new float[] {start, end} : null;
    }

    /**
     * Cubic ease-in: e = a u + (1 - a) u^3 with u = clamp(seconds / CLEAR_SECONDS) and a = {@link #EASE_LINEAR}. It
     * starts slow, speeds up toward the end, moves from the first frame, and is monotonic: no hold, no dip.
     */
    static double easeAt(double seconds) {
        double u = Math.max(0.0, Math.min(1.0, seconds / CLEAR_SECONDS));
        return EASE_LINEAR * u + (1.0 - EASE_LINEAR) * u * u * u;
    }

    /** Portal view: V, and never above the real DH distance. */
    static int portalViewDistance(int vanilla, int real) {
        return vanilla <= 0 ? real : Math.min(vanilla, real);
    }

    /** d = V x (L / V)^e, rounded, never above L. When V >= L there is nothing to fade. */
    static int distance(int vanilla, int lod, double e) {
        if (vanilla <= 0 || vanilla >= lod) {
            return lod;
        }
        double d = vanilla * Math.pow((double) lod / vanilla, e);
        return (int) Math.min(lod, Math.round(d));
    }
}
