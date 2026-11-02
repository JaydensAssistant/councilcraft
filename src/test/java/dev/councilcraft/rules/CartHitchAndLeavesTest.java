package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.cart.CartDetachRules;
import dev.councilcraft.cart.CartLeavesRules;
import org.junit.jupiter.api.Test;

/** CC-B24 hitch rope + CC-174 cart leaf pass-through unit coverage. */
public class CartHitchAndLeavesTest {
    private static final double EPS = 1e-9;

    @Test
    void ropeTautOnlyPastHitchSlack() {
        double spacing = 2.0;
        assertEquals(spacing + CartDetachRules.HITCH_SLACK, CartDetachRules.hitchMaxDistance(spacing), EPS);
        assertFalse(CartDetachRules.isRopeTaut(spacing, spacing));
        assertFalse(CartDetachRules.isRopeTaut(spacing + CartDetachRules.HITCH_SLACK, spacing));
        assertTrue(CartDetachRules.isRopeTaut(spacing + CartDetachRules.HITCH_SLACK + 0.01, spacing));
    }

    @Test
    void clampScalesPullerOntoHitchCircle() {
        assertEquals(1.0, CartDetachRules.clampHorizontalScale(1.0, 2.0), EPS);
        assertEquals(1.0, CartDetachRules.clampHorizontalScale(2.5, 2.0), EPS);
        assertEquals(0.5, CartDetachRules.clampHorizontalScale(5.0, 2.0), EPS);
        // 0 distance: leave scale at 1 (caller must guard divide-by-zero elsewhere).
        assertEquals(1.0, CartDetachRules.clampHorizontalScale(0.0, 2.0), EPS);
    }

    @Test
    void outwardVelocityCancelledAtHitchInwardFree() {
        // At hitch max, moving away (+x): cancel full outward component.
        assertEquals(0.8, CartDetachRules.outwardSpeedToCancel(2.5, 2.0, 1, 0, 0.8, 0.3), EPS);
        // Sideways-only at hitch: nothing to cancel.
        assertEquals(0.0, CartDetachRules.outwardSpeedToCancel(2.5, 2.0, 1, 0, 0.0, 0.5), EPS);
        // Moving back toward cart: free.
        assertEquals(0.0, CartDetachRules.outwardSpeedToCancel(2.5, 2.0, 1, 0, -0.9, 0), EPS);
        // Slack rope: free even if facing outward.
        assertEquals(0.0, CartDetachRules.outwardSpeedToCancel(2.0, 2.0, 1, 0, 1.0, 0), EPS);
        // Past hitch: still cancel outward.
        assertEquals(1.2, CartDetachRules.outwardSpeedToCancel(4.0, 2.0, 0, 1, 0.0, 1.2), EPS);
    }

    @Test
    void neverAutoUnhitch() {
        assertFalse(CartDetachRules.allowCollisionDetach());
        assertFalse(CartDetachRules.allowDistanceDetach(100.0, 2.0));
    }

    @Test
    void niftyCartsPassThroughLeavesNotPostilion() {
        assertTrue(CartLeavesRules.passesThroughLeaves("niftycarts:supply_cart"));
        assertTrue(CartLeavesRules.passesThroughLeaves("niftycarts:animal_cart"));
        assertTrue(CartLeavesRules.passesThroughLeaves("niftycarts:plow"));
        assertTrue(CartLeavesRules.passesThroughLeaves("niftycarts:hand_cart"));
        assertTrue(CartLeavesRules.passesThroughLeaves("niftycarts:seed_drill"));
        assertTrue(CartLeavesRules.passesThroughLeaves("niftycarts:reaper"));
        assertTrue(CartLeavesRules.passesThroughLeaves("niftycarts:wagon"));
        assertFalse(CartLeavesRules.passesThroughLeaves("niftycarts:postilion"));
        assertFalse(CartLeavesRules.passesThroughLeaves("minecraft:horse"));
        assertFalse(CartLeavesRules.passesThroughLeaves("minecraft:oak_boat"));
        assertFalse(CartLeavesRules.passesThroughLeaves(null));
    }
}
