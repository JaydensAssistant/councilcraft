package dev.councilcraft.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** PT45: no hold, 3 s cubic ease-in from the portal view's fog distance V (or vanilla fog) to the real distance. */
class PortalFadeTest {
    @Test
    void timingsMatchTheLock() {
        assertEquals(3.0, PortalFade.CLEAR_SECONDS);
        assertEquals(8, PortalFade.IP_CAP_DEFAULT);
    }

    @Test
    void theDestinationHoldIsGone() {
        for (String gone : new String[] {"HOLD_CAP_SECONDS", "holding", "clearNanos", "IN_SECONDS", "DIP", "DIP_NETHER"}) {
            assertTrue(Arrays.stream(PortalFade.class.getDeclaredFields()).noneMatch(f -> f.getName().equals(gone)), gone);
        }
        assertTrue(Arrays.stream(PortalFade.class.getDeclaredMethods()).noneMatch(m -> m.getName().equals("levelLoaded")));
        assertTrue(Arrays.stream(PortalFade.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()))
                .allMatch(f -> f.getName().startsWith("CLEAR") || f.getName().startsWith("EASE") || f.getName().startsWith("IP_")));
    }

    @Test
    void easeIsACubicEaseInThatMovesFromTheFirstFrame() {
        assertEquals(0.0, PortalFade.easeAt(-1.0), 1e-12);
        assertEquals(0.0, PortalFade.easeAt(0.0), 1e-12);
        assertEquals(1.0, PortalFade.easeAt(3.0), 1e-12);
        assertEquals(1.0, PortalFade.easeAt(9.0), 1e-12);
        // u = 0.5: 0.2 * 0.5 + 0.8 * 0.125 = 0.2
        assertEquals(0.2, PortalFade.easeAt(1.5), 1e-12);
        // Moves from the first frame (one 60 fps frame in), not a pause.
        assertTrue(PortalFade.easeAt(1.0 / 60) > 0.0);
        assertTrue(PortalFade.easeAt(0.1) > 0.005);
        // Ease-in: slower than linear for the whole clear, steeper at the end than at the start.
        for (double t = 0.01; t < 3.0; t += 0.01) {
            assertTrue(PortalFade.easeAt(t) < t / 3.0, "below linear at t=" + t);
        }
        double early = PortalFade.easeAt(0.3) - PortalFade.easeAt(0.0);
        double late = PortalFade.easeAt(3.0) - PortalFade.easeAt(2.7);
        assertTrue(late > 3 * early, "speeds up toward the end");
        double previous = -1;
        for (double t = 0; t <= 3.0; t += 0.005) {
            double e = PortalFade.easeAt(t);
            assertTrue(e >= previous, "monotonic: no dip, no fade-in, no hold");
            previous = e;
        }
    }

    @Test
    void distanceRecedesOnALogScaleFromVToL() {
        int v = 128; // 8 chunks
        int l = 4096; // 256 chunks
        assertEquals(v, PortalFade.distance(v, l, 0.0));
        assertEquals(l, PortalFade.distance(v, l, 1.0));
        // Geometric midpoint at e = 0.5: sqrt(128 * 4096) = 724.08
        assertEquals(724, PortalFade.distance(v, l, 0.5));
        int previous = 0;
        for (double e = 0; e <= 1.0; e += 0.01) {
            int d = PortalFade.distance(v, l, e);
            assertTrue(d >= previous && d <= l, "monotonic and never above the real distance");
            previous = d;
        }
    }

    @Test
    void theFadeStartsExactlyAtVSoThereIsNoSnap() {
        // The portal view shows V; the first frame after the crossing shows distance(V, L, ease(0)) = V.
        assertEquals(128, PortalFade.portalViewDistance(128, 4096));
        assertEquals(PortalFade.portalViewDistance(128, 4096), PortalFade.distance(128, 4096, PortalFade.easeAt(0.0)));
    }

    @Test
    void portalViewShowsVAndNeverMoreThanTheRealDistance() {
        assertEquals(128, PortalFade.portalViewDistance(128, 4096));
        assertEquals(100, PortalFade.portalViewDistance(128, 100));
        assertEquals(100, PortalFade.portalViewDistance(0, 100));
    }

    @Test
    void nothingToFadeWhenTheRealEdgeIsAlreadyFurther() {
        assertEquals(100, PortalFade.distance(192, 100, 0.0));
        assertEquals(100, PortalFade.distance(0, 100, 0.0));
    }

    @Test
    void vanillaFogMatchesBackgroundRendererForNormalAndThickFog() {
        // Overworld, render distance 12 (192 blocks): end = vd, start = vd - clamp(vd / 10, 4, 64).
        float[] normal = PortalFade.vanillaFog(192.0F, false);
        assertEquals(172.8F, normal[0], 1e-3F);
        assertEquals(192.0F, normal[1], 1e-3F);
        // Large view distance: the start gap is capped at 64 blocks.
        float[] far = PortalFade.vanillaFog(1024.0F, false);
        assertEquals(960.0F, far[0], 1e-3F);
        assertEquals(1024.0F, far[1], 1e-3F);
        // Tiny view distance: the gap is at least 4 blocks.
        assertEquals(28.0F, PortalFade.vanillaFog(32.0F, false)[0], 1e-3F);
        // Nether (thick fog): start 5% of vd, end half of min(vd, 192).
        float[] thick = PortalFade.vanillaFog(192.0F, true);
        assertEquals(9.6F, thick[0], 1e-3F);
        assertEquals(96.0F, thick[1], 1e-3F);
        assertEquals(96.0F, PortalFade.vanillaFog(1024.0F, true)[1], 1e-3F);
        assertEquals(64.0F, PortalFade.vanillaFog(128.0F, true)[1], 1e-3F);
    }

    @Test
    void vanillaFogBecomesDhFarFogFractions() {
        // Overworld, vd 192 against a 256-chunk (4096 block) LOD distance.
        float[] f = PortalFade.fogFractions(172.8F, 192.0F, 4096);
        assertEquals(172.8F / 4096, f[0], 1e-6F);
        assertEquals(192.0F / 4096, f[1], 1e-6F);
        // Nether thick fog.
        float[] n = PortalFade.fogFractions(9.6F, 96.0F, 4096);
        assertEquals(96.0F / 4096, n[1], 1e-6F);
        // Fog already past the LOD range: nothing to fade. Bad LOD distances are ignored.
        assertEquals(null, PortalFade.fogFractions(5000F, 6000F, 4096));
        assertEquals(null, PortalFade.fogFractions(0F, 100F, 0));
        // The end is clamped to 1.0.
        assertEquals(1.0F, PortalFade.fogFractions(100F, 9000F, 4096)[1], 1e-6F);
    }
}
