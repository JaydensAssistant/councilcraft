package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.enchant.ForgerLevels;
import dev.councilcraft.respawn.RebornClock;
import dev.councilcraft.respawn.RebornStats;
import dev.councilcraft.respawn.RebornText;
import org.junit.jupiter.api.Test;

/** Baby respawn curve, the tooltip numbers, and the Blaze Forger level clamp. */
class RespawnRulesTest {
    @Test
    void rebornStartsSmallAndIsBackToNormalAfterOneRealDay() {
        RebornStats start = RebornStats.at(0L);
        assertEquals(CouncilCraftRules.REBORN_SCALE, start.scale, 0.0001f);
        assertEquals(CouncilCraftRules.REBORN_HEARTS, start.hearts);
        assertEquals(CouncilCraftRules.REBORN_SPEED, start.speed, 0.0001f);
        assertEquals(CouncilCraftRules.REBORN_MELEE, start.melee, 0.0001f);

        long almostSixHours = CouncilCraftRules.REBORN_HEART_MILLIS - 1L;
        assertEquals(CouncilCraftRules.REBORN_HEARTS, RebornStats.at(almostSixHours).hearts);
        assertEquals(CouncilCraftRules.REBORN_HEARTS + 1, RebornStats.at(CouncilCraftRules.REBORN_HEART_MILLIS).hearts);

        RebornStats done = RebornStats.at(CouncilCraftRules.REBORN_MILLIS);
        assertEquals(CouncilCraftRules.VANILLA_HEARTS, done.hearts);
        assertEquals(1f, done.scale, 0.0001f);
        assertEquals(1f, done.speed, 0.0001f);
        assertEquals(1f, done.melee, 0.0001f);
        assertEquals(done.hearts, RebornStats.at(CouncilCraftRules.REBORN_MILLIS + 60_000L).hearts);
    }

    @Test
    void sizeSpeedAndStrengthEaseInAStraightLine() {
        long halfway = CouncilCraftRules.REBORN_MILLIS / 2L;
        RebornStats mid = RebornStats.at(halfway);
        assertEquals(0.8f, mid.scale, 0.001f);
        assertEquals(0.925f, mid.speed, 0.001f);
        assertEquals(0.85f, mid.melee, 0.001f);
        assertEquals(8, mid.hearts);
    }

    @Test
    void tooltipPercentsComeFromTheSameConstants() {
        int[] shown = RebornText.startingPercents();
        assertEquals(RebornText.percent(CouncilCraftRules.REBORN_SCALE), shown[0]);
        assertEquals(RebornText.percent(CouncilCraftRules.REBORN_SPEED), shown[1]);
        assertEquals(RebornText.percent(CouncilCraftRules.REBORN_MELEE), shown[2]);
        assertEquals(RebornText.hpPercent(CouncilCraftRules.REBORN_HEARTS), shown[3]);
        // 6 of 10 hearts is 60% HP. The language line is these four numbers.
        assertArrayEquals(new int[] {60, 85, 70, 60}, shown);
    }

    @Test
    void nowLineUsesTheLiveCurve() {
        assertArrayEquals(RebornText.startingPercents(), RebornText.nowPercents(RebornStats.at(0L)));
        RebornStats twelveHours = RebornStats.at(CouncilCraftRules.REBORN_HEART_MILLIS * 2L);
        assertArrayEquals(new int[] {80, 93, 85, 80}, RebornText.nowPercents(twelveHours));
    }

    @Test
    void clientClockFollowsTheServerExpiry() {
        long day = CouncilCraftRules.REBORN_MILLIS;
        long serverNow = 1_700_000_000_000L;
        long until = serverNow + day;
        long clientAtSync = 50_000L;
        assertEquals(0L, RebornClock.elapsed(until, serverNow, clientAtSync, clientAtSync));
        assertEquals(60_000L, RebornClock.elapsed(until, serverNow, clientAtSync, clientAtSync + 60_000L));
        assertEquals(day, RebornClock.elapsed(0L, serverNow, clientAtSync, clientAtSync));
    }

    @Test
    void twoBooksAtTheVanillaMaxDoNotCombineHigher() {
        assertEquals(5, ForgerLevels.clamp(true, 5, 5, 5, 6));
        assertEquals(5, ForgerLevels.clamp(false, 5, 5, 5, 6));
        assertEquals(4, ForgerLevels.clamp(true, 5, 3, 4, 4));
    }

    @Test
    void onlyFourEnchantmentsMaySitOneAboveTheVanillaMax() {
        assertEquals(6, ForgerLevels.clamp(true, 5, 6, 5, 7));
        assertEquals(5, ForgerLevels.clamp(false, 5, 6, 5, 7));
        assertTrue(ForgerLevels.mayExceedVanillaMax(CouncilCraftRules.SHARPNESS_ID));
        assertTrue(ForgerLevels.mayExceedVanillaMax(CouncilCraftRules.POWER_ID));
        assertTrue(ForgerLevels.mayExceedVanillaMax(CouncilCraftRules.BLAST_PROTECTION_ID));
        assertTrue(ForgerLevels.mayExceedVanillaMax(CouncilCraftRules.PROTECTION_ID));
        assertFalse(ForgerLevels.mayExceedVanillaMax("minecraft:efficiency"));
        assertFalse(ForgerLevels.mayExceedVanillaMax("minecraft:feather_falling"));
    }
}
