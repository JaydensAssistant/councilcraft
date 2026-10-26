package dev.councilcraft.rules;

import static dev.councilcraft.rules.CarryEscapeRules.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** PT22 amendment: escape cooldown 5 s doubling, capped at 30 min, reset after 2x quiet. */
class CarryEscapeRulesTest {
    private static final long S = TICKS_PER_SECOND;

    @Test
    void cooldownDoublesFromFiveSecondsToThirtyMinutes() {
        assertEquals(0, cooldownTicks(0));
        assertEquals(5 * S, cooldownTicks(1));
        assertEquals(10 * S, cooldownTicks(2));
        assertEquals(20 * S, cooldownTicks(3));
        assertEquals(40 * S, cooldownTicks(4));
        assertEquals(80 * S, cooldownTicks(5));
        assertEquals(1280 * S, cooldownTicks(9));
        assertEquals(1800 * S, cooldownTicks(10));
        assertEquals(1800 * S, cooldownTicks(64));
    }

    @Test
    void escapeWithinTwiceTheCooldownRaisesTheLevel() {
        assertEquals(1, levelAfterEscape(0, 0, 1000));
        assertEquals(2, levelAfterEscape(1, 0, 10 * S));
        assertEquals(1, levelAfterEscape(1, 0, 10 * S + 1));
        assertEquals(3, levelAfterEscape(2, 0, 20 * S));
        assertEquals(1, levelAfterEscape(10, 0, 3600 * S + 1));
        assertEquals(11, levelAfterEscape(10, 0, 3600 * S));
    }

    @Test
    void ticksLeftAndExpiry() {
        assertEquals(5 * S, ticksLeft(1, 100, 100));
        assertEquals(1, ticksLeft(1, 100, 100 + 5 * S - 1));
        assertEquals(0, ticksLeft(1, 100, 100 + 5 * S));
        assertEquals(0, ticksLeft(0, 0, 0));
        assertFalse(expired(1, 0, 10 * S));
        assertTrue(expired(1, 0, 10 * S + 1));
    }

    @Test
    void formatRoundsUpToTheSecond() {
        assertEquals("5s", format(5 * S));
        assertEquals("1s", format(1));
        assertEquals("0s", format(0));
        assertEquals("1m 20s", format(80 * S));
        assertEquals("2m 30s", format(150 * S - 5));
        assertEquals("30m", format(1800 * S));
    }
}
