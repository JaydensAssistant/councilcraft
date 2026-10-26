package dev.councilcraft.compat.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/**
 * PT13. After the player changes dimension (an Immersive Portals crossing, or any other dimension change),
 * Distant Horizons terrain starts hidden in fog at the vanilla chunk edge and the fog rolls back out to the
 * normal DH distance over {@link #SECONDS}, so the LODs fade in instead of popping. With shaders the fog
 * distance reaches the shader pack through Iris' dhRenderDistance uniform ({@code IrisPortalFadeUniformMixin});
 * without shaders through DH's own fog ({@code PortalFadeDhFog}). Client only; loaded only when Distant Horizons
 * and Immersive Portals are present.
 */
public final class PortalFade {
    /** Length of the fade. */
    static final double SECONDS = 3.0;

    private static long startNanos;
    private static boolean active;
    private static RegistryKey<World> lastDimension;

    private PortalFade() {
    }

    /** Starts the fade. Called at the end of IP's ClientTeleportationManager.changePlayerDimension. */
    public static void begin() {
        MinecraftClient client = MinecraftClient.getInstance();
        lastDimension = client.world == null ? null : client.world.getRegistryKey();
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
            startNanos = System.nanoTime();
            active = true;
        } else {
            // Joining or leaving a world is not a crossing.
            active = false;
        }
    }

    /** 1 right after the crossing, falling to 0 when the fade is over; 0 outside a fade. */
    public static float fade() {
        if (!active) {
            return 0.0F;
        }
        double seconds = (System.nanoTime() - startNanos) / 1.0E9;
        if (seconds >= SECONDS) {
            active = false;
            return 0.0F;
        }
        return (float) fadeAt(seconds);
    }

    /** The dhRenderDistance (blocks) shaders should see: the vanilla edge at full fade, the real value at 0. */
    public static int effectiveDhRenderDistance(int real) {
        float fade = fade();
        if (fade <= 0.0F || PortalDhDepth.inPortalView()) {
            return real;
        }
        return lerpDistance(real, MinecraftClient.getInstance().options.getClampedViewDistance() * 16, fade);
    }

    /** 1 - smoothstep(0, SECONDS, seconds). */
    static double fadeAt(double seconds) {
        double x = Math.max(0.0, Math.min(1.0, seconds / SECONDS));
        return 1.0 - x * x * (3.0 - 2.0 * x);
    }

    /** Moves the DH distance toward the vanilla edge by fade (0..1); never pushes it outward. */
    static int lerpDistance(int real, int vanilla, double fade) {
        if (vanilla >= real) {
            return real;
        }
        return (int) Math.round(real + (vanilla - real) * fade);
    }
}
