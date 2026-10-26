package dev.councilcraft.compat.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/**
 * PT13 (+ amendment). After the player changes dimension (an Immersive Portals crossing, or any other dimension
 * change), the far fog eases in from the normal DH distance (what the portal view just showed) over
 * {@link #IN_SECONDS}, holds until {@link #HOLD_END_SECONDS} so the new dimension's fog color settles, then
 * clears back out to the normal DH distance by {@link #END_SECONDS} with an ease-out curve on the distance.
 * The dip is partial: {@link #DIP} of the way from the DH distance toward the vanilla chunk edge
 * ({@link #DIP_NETHER} in the Nether). All timings and depths are tunable constants. With shaders the fog
 * distance reaches the shader pack through Iris' dhRenderDistance uniform ({@code IrisPortalFadeUniformMixin});
 * without shaders through DH's own fog ({@code PortalFadeDhFog}). Client only; loaded only when Distant Horizons
 * and Immersive Portals are present.
 */
public final class PortalFade {
    /** End of the ease-in (smoothstep 0 -> dip). */
    static final double IN_SECONDS = 0.5;
    /** End of the hold at full dip. */
    static final double HOLD_END_SECONDS = 1.0;
    /** End of the clear (ease-out back to the normal distance). */
    static final double END_SECONDS = 5.5;
    /** How far the fog distance is pulled from the DH distance toward the vanilla edge, Overworld / End. */
    static final double DIP = 0.5;
    /** Same in the Nether (shallower: packs scale Nether fog with the render distance). */
    static final double DIP_NETHER = 0.3;

    private static long startNanos;
    private static boolean active;
    private static boolean nether;
    private static RegistryKey<World> lastDimension;

    private PortalFade() {
    }

    /** Starts the fade. Called at the end of IP's ClientTeleportationManager.changePlayerDimension. */
    public static void begin() {
        MinecraftClient client = MinecraftClient.getInstance();
        lastDimension = client.world == null ? null : client.world.getRegistryKey();
        nether = World.NETHER.equals(lastDimension);
        startNanos = System.nanoTime();
        active = true;
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
            nether = World.NETHER.equals(dimension);
            startNanos = System.nanoTime();
            active = true;
        } else {
            // Joining or leaving a world is not a crossing.
            active = false;
        }
    }

    /** 0 at the crossing, up to the dip, back to 0 when the fade is over; 0 outside a fade. */
    public static float fade() {
        if (!active) {
            return 0.0F;
        }
        double seconds = (System.nanoTime() - startNanos) / 1.0E9;
        if (seconds >= END_SECONDS) {
            active = false;
            return 0.0F;
        }
        return (float) fadeAt(seconds, nether ? DIP_NETHER : DIP);
    }

    /** The dhRenderDistance (blocks) shaders should see: the real value at 0, toward the vanilla edge by fade. */
    public static int effectiveDhRenderDistance(int real) {
        float fade = fade();
        if (fade <= 0.0F || PortalDhDepth.inPortalView()) {
            return real;
        }
        return lerpDistance(real, MinecraftClient.getInstance().options.getClampedViewDistance() * 16, fade);
    }

    /**
     * The fade shape: smoothstep 0 -> dip over [0, IN], hold dip until HOLD_END, then the distance eases out
     * (1 - (1 - u)^2, fast at first, slowing as it reaches the normal distance) back to 0 by END.
     */
    static double fadeAt(double seconds, double dip) {
        if (seconds <= 0.0 || seconds >= END_SECONDS) {
            return 0.0;
        }
        if (seconds < IN_SECONDS) {
            double x = seconds / IN_SECONDS;
            return dip * x * x * (3.0 - 2.0 * x);
        }
        if (seconds < HOLD_END_SECONDS) {
            return dip;
        }
        double u = (seconds - HOLD_END_SECONDS) / (END_SECONDS - HOLD_END_SECONDS);
        double cleared = 1.0 - (1.0 - u) * (1.0 - u);
        return dip * (1.0 - cleared);
    }

    /** Moves the DH distance toward the vanilla edge by fade (0..1); never pushes it outward. */
    static int lerpDistance(int real, int vanilla, double fade) {
        if (vanilla >= real) {
            return real;
        }
        return (int) Math.round(real + (vanilla - real) * fade);
    }
}
