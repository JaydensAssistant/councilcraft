package dev.councilcraft.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** PT13 (+ amendment) fade curve and distance blend. */
class PortalFadeTest {
    @Test
    void timingsAndDipsMatchTheAmendment() {
        assertEquals(0.5, PortalFade.IN_SECONDS);
        assertEquals(1.0, PortalFade.HOLD_END_SECONDS);
        assertEquals(5.5, PortalFade.END_SECONDS);
        assertEquals(0.5, PortalFade.DIP);
        assertTrue(PortalFade.DIP_NETHER < PortalFade.DIP, "shallower in the Nether");
    }

    @Test
    void startsAtTheRealDistanceAndEasesIn() {
        // No jump on crossing: fade 0 means the real DH distance (what the portal view showed).
        assertEquals(0.0, PortalFade.fadeAt(0.0, 0.5), 1e-9);
        assertEquals(0.0, PortalFade.fadeAt(0.005, 0.5), 1e-3);
        assertEquals(0.25, PortalFade.fadeAt(0.25, 0.5), 1e-9);
        assertEquals(0.5, PortalFade.fadeAt(0.5, 0.5), 1e-9);
        double previous = -1;
        for (double t = 0; t <= 0.5; t += 0.01) {
            double f = PortalFade.fadeAt(t, 0.5);
            assertTrue(f >= previous, "monotonic ease-in");
            previous = f;
        }
    }

    @Test
    void holdsThenClearsWithAnEaseOutByFiveAndAHalfSeconds() {
        assertEquals(0.5, PortalFade.fadeAt(0.75, 0.5), 1e-9);
        assertEquals(0.5, PortalFade.fadeAt(0.999, 0.5), 1e-9);
        // Halfway through the clear (u = 0.5): 1 - (1 - 0.5)^2 = 0.75 cleared.
        assertEquals(0.125, PortalFade.fadeAt(3.25, 0.5), 1e-9);
        // Ease-out: the first half of the clear covers more than the second.
        double firstHalf = PortalFade.fadeAt(1.0, 0.5) - PortalFade.fadeAt(3.25, 0.5);
        double secondHalf = PortalFade.fadeAt(3.25, 0.5) - PortalFade.fadeAt(5.5, 0.5);
        assertTrue(firstHalf > secondHalf);
        assertEquals(0.0, PortalFade.fadeAt(5.49, 0.5), 1e-4);
        assertEquals(0.0, PortalFade.fadeAt(5.5, 0.5), 1e-9);
        assertEquals(0.0, PortalFade.fadeAt(10.0, 0.5), 1e-9);
        assertEquals(0.0, PortalFade.fadeAt(-1.0, 0.5), 1e-9);
        // Nether dip is the lower peak.
        assertEquals(0.3, PortalFade.fadeAt(0.8, PortalFade.DIP_NETHER), 1e-9);
    }

    @Test
    void distanceMovesPartWayTowardTheVanillaEdge() {
        assertEquals(4096, PortalFade.lerpDistance(4096, 192, 0.0));
        // Full dip: about halfway in, not to the vanilla chunk edge.
        assertEquals(2144, PortalFade.lerpDistance(4096, 192, PortalFade.DIP));
        assertEquals(192, PortalFade.lerpDistance(4096, 192, 1.0));
        // Never pushes the DH distance outward.
        assertEquals(100, PortalFade.lerpDistance(100, 192, 1.0));
    }
}
