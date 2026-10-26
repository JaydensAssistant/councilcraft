package dev.councilcraft.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** PT31: monotonic log-scale clear from the real-chunk edge V to the DH distance L. */
class PortalFadeTest {
    @Test
    void timingsMatchTheLock() {
        assertEquals(5.0, PortalFade.CLEAR_SECONDS);
        assertEquals(1.0, PortalFade.HOLD_CAP_SECONDS);
        assertEquals(8, PortalFade.IP_CAP_DEFAULT);
    }

    @Test
    void oldDipConstantsAreGone() {
        for (String gone : new String[] {"IN_SECONDS", "HOLD_END_SECONDS", "END_SECONDS", "DIP", "DIP_NETHER"}) {
            assertTrue(Arrays.stream(PortalFade.class.getDeclaredFields()).noneMatch(f -> f.getName().equals(gone)), gone);
        }
        assertTrue(Arrays.stream(PortalFade.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()))
                .allMatch(f -> f.getName().startsWith("CLEAR") || f.getName().startsWith("HOLD") || f.getName().startsWith("IP_")));
    }

    @Test
    void easeIsCubicOutAndMonotonic() {
        assertEquals(0.0, PortalFade.easeAt(-1.0), 1e-12);
        assertEquals(0.0, PortalFade.easeAt(0.0), 1e-12);
        assertEquals(0.875, PortalFade.easeAt(2.5), 1e-12); // u = 0.5: 1 - 0.5^3
        assertEquals(1.0, PortalFade.easeAt(5.0), 1e-12);
        assertEquals(1.0, PortalFade.easeAt(9.0), 1e-12);
        double previous = -1;
        for (double t = 0; t <= 5.0; t += 0.01) {
            double e = PortalFade.easeAt(t);
            assertTrue(e >= previous, "monotonic: no dip, no fade-in");
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
    void nothingToFadeWhenTheRealEdgeIsAlreadyFurther() {
        assertEquals(100, PortalFade.distance(192, 100, 0.0));
        assertEquals(100, PortalFade.distance(0, 100, 0.0));
    }
}
