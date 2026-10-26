package dev.councilcraft.compat.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/**
 * PT31 (replaces the PT13 dip). After the player changes dimension (an Immersive Portals crossing, or any other
 * dimension change) the Distant Horizons edge starts at the real-chunk edge the portal view showed and recedes,
 * monotonically, to the normal DH distance:
 * <ul>
 * <li>V = 16 x min(clamped client view distance (already capped by the server's), IP's indirect loading cap) blocks;</li>
 * <li>L = DH lodChunkRenderDistanceRadius x 16 blocks;</li>
 * <li>u = clamp(t / {@link #CLEAR_SECONDS}), counted after the destination hold; e = 1 - (1 - u)^3;</li>
 * <li>d = V x (L / V)^e (log scale, so the edge recedes evenly).</li>
 * </ul>
 * Destination hold: u stays 0 until Distant Horizons reports the destination level loaded, at most
 * {@link #HOLD_CAP_SECONDS}. With shaders the distance reaches the pack through Iris' dhRenderDistance uniform
 * ({@code IrisPortalFadeUniformMixin}); without shaders through DH's own fog ({@code PortalFadeDhFog}). Client only;
 * loaded only when Distant Horizons and Immersive Portals are present.
 */
public final class PortalFade {
    /** PT31 amendment: the clear takes 5.0 s. */
    static final double CLEAR_SECONDS = 5.0;
    /** Longest wait for DH to load the destination level before the clear starts. */
    static final double HOLD_CAP_SECONDS = 1.0;
    /** Immersive Portals' default indirectLoadingRadiusCap (chunks), used when IPGlobal can't be read. */
    static final int IP_CAP_DEFAULT = 8;

    private static long startNanos;
    private static long clearNanos;
    private static boolean holding;
    private static boolean active;
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
        active = true;
        holding = destination == null || !PortalFadeLevels.isLoaded(destination.getValue().toString());
        clearNanos = holding ? startNanos + (long) (HOLD_CAP_SECONDS * 1.0E9) : startNanos;
    }

    /** DH loaded a client level: if it is the destination, the clear starts now (no later than the cap). */
    public static void levelLoaded(String dimension) {
        if (!active || !holding || lastDimension == null || !lastDimension.getValue().toString().equals(dimension)) {
            return;
        }
        holding = false;
        clearNanos = Math.min(clearNanos, System.nanoTime());
    }

    /** Eased clear progress e (0 at the crossing and during the hold, 1 when done); 1 outside a fade. */
    public static float progress() {
        if (!active) {
            return 1.0F;
        }
        long now = System.nanoTime();
        double e = easeAt((now - clearNanos) / 1.0E9);
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

    /** The dhRenderDistance (blocks) shaders should see. Never above the real value. */
    public static int effectiveDhRenderDistance(int real) {
        if (!fading()) {
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

    /** e = 1 - (1 - u)^3 with u = clamp(seconds / CLEAR_SECONDS). Monotonic: no fade-in, no hold, no dip. */
    static double easeAt(double seconds) {
        double u = Math.max(0.0, Math.min(1.0, seconds / CLEAR_SECONDS));
        double r = 1.0 - u;
        return 1.0 - r * r * r;
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
