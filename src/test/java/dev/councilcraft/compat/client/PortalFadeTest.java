package dev.councilcraft.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** PT13 fade curve and distance blend. */
class PortalFadeTest {
    @Test
    void fadeIsASmoothstepOverThreeSeconds() {
        assertEquals(1.0, PortalFade.fadeAt(0.0), 1e-9);
        assertEquals(0.5, PortalFade.fadeAt(1.5), 1e-9);
        assertEquals(0.0, PortalFade.fadeAt(3.0), 1e-9);
        assertEquals(0.0, PortalFade.fadeAt(10.0), 1e-9);
        assertEquals(1.0, PortalFade.fadeAt(-1.0), 1e-9);
        // Smooth at both ends.
        assertEquals(1.0, PortalFade.fadeAt(0.01), 1e-4);
        assertEquals(0.0, PortalFade.fadeAt(2.99), 1e-4);
    }

    @Test
    void distanceMovesFromTheVanillaEdgeToTheRealDistance() {
        assertEquals(192, PortalFade.lerpDistance(4096, 192, 1.0));
        assertEquals(4096, PortalFade.lerpDistance(4096, 192, 0.0));
        assertEquals(2144, PortalFade.lerpDistance(4096, 192, 0.5));
        // Never pushes the DH distance outward.
        assertEquals(100, PortalFade.lerpDistance(100, 192, 1.0));
    }
}
