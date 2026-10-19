package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.enchant.ForgerLevels;
import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.respawn.Reborn.JoinStep;
import dev.councilcraft.respawn.RebornClock;
import dev.councilcraft.respawn.RebornEmi;
import dev.councilcraft.respawn.RebornProgress;
import dev.councilcraft.respawn.RebornStats;
import dev.councilcraft.respawn.RebornText;
import java.nio.file.Files;
import java.nio.file.Path;
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
        assertEquals(CouncilCraftRules.REBORN_HUNGER, start.hunger, 0.0001f);

        long almostSixHours = CouncilCraftRules.REBORN_HEART_MILLIS - 1L;
        assertEquals(CouncilCraftRules.REBORN_HEARTS, RebornStats.at(almostSixHours).hearts);
        assertEquals(CouncilCraftRules.REBORN_HEARTS + 1, RebornStats.at(CouncilCraftRules.REBORN_HEART_MILLIS).hearts);

        RebornStats done = RebornStats.at(CouncilCraftRules.REBORN_MILLIS);
        assertEquals(CouncilCraftRules.VANILLA_HEARTS, done.hearts);
        assertEquals(1f, done.scale, 0.0001f);
        assertEquals(1f, done.speed, 0.0001f);
        assertEquals(1f, done.melee, 0.0001f);
        assertEquals(1f, done.hunger, 0.0001f);
        assertEquals(done.hearts, RebornStats.at(CouncilCraftRules.REBORN_MILLIS + 60_000L).hearts);
    }

    @Test
    void sizeSpeedAndStrengthEaseInAStraightLine() {
        long halfway = CouncilCraftRules.REBORN_MILLIS / 2L;
        RebornStats mid = RebornStats.at(halfway);
        assertEquals(0.8f, mid.scale, 0.001f);
        assertEquals(0.925f, mid.speed, 0.001f);
        assertEquals(0.85f, mid.melee, 0.001f);
        assertEquals(1.25f, mid.hunger, 0.001f);
        assertEquals(8, mid.hearts);
    }

    @Test
    void hungerExhaustionEasesFromOneAndAHalfToNormal() {
        assertEquals(0f, RebornProgress.fraction(0L), 0.0001f);
        assertEquals(0.5f, RebornProgress.fraction(CouncilCraftRules.REBORN_MILLIS / 2L), 0.0001f);
        assertEquals(1f, RebornProgress.fraction(CouncilCraftRules.REBORN_MILLIS), 0.0001f);
        assertEquals(1f, RebornProgress.fraction(CouncilCraftRules.REBORN_MILLIS + 60_000L), 0.0001f);

        long halfway = CouncilCraftRules.REBORN_MILLIS / 2L;
        RebornStats mid = RebornStats.at(halfway);
        float scaleProgress = (mid.scale - CouncilCraftRules.REBORN_SCALE) / (1f - CouncilCraftRules.REBORN_SCALE);
        assertEquals(scaleProgress, mid.progress, 0.0001f);
        assertEquals(1.25f, mid.hunger, 0.0001f);
        assertEquals(2.5f, RebornStats.exhaustion(2f, mid.hunger), 0.0001f);
        assertEquals(1.5f, RebornStats.exhaustion(1f, RebornStats.at(0L).hunger), 0.0001f);
        assertEquals(1f, RebornStats.exhaustion(1f, RebornStats.at(CouncilCraftRules.REBORN_MILLIS).hunger), 0.0001f);
        assertEquals(0f, RebornStats.exhaustion(0f, CouncilCraftRules.REBORN_HUNGER), 0.0001f);
        assertEquals(-2f, RebornStats.exhaustion(-2f, CouncilCraftRules.REBORN_HUNGER), 0.0001f);
    }

    @Test
    void hoverIsTheSentenceAndTheLiveLine() throws Exception {
        String lang = Files.readString(Path.of("src/main/resources/assets/councilcraft/lang/en_us.json"));
        assertTrue(lang.contains("Incrementally return to adult stats over one real life day, on or offline."));
        assertTrue(lang.contains("Now: %s%% size, %s%% speed, %s%% strength, %s%% HP"));
        assertFalse(lang.contains("effect.councilcraft.reborn.line2"));
        assertEquals(RebornText.LINE1, "effect.councilcraft.reborn.line1");
        assertEquals(RebornText.NOW, "effect.councilcraft.reborn.now");
        // The floor numbers stay in the constants. They are not a hover line.
        assertArrayEquals(new int[] {60, 85, 70, 60}, RebornText.startingPercents());
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
        // Sharpness V + V clamps back to V, which is not an improvement.
        assertFalse(ForgerLevels.clamp(true, 5, 5, 5, 6) > 5);
    }

    @Test
    void joinSendsNoStatusEffect() {
        long now = 1_700_000_000_000L;
        assertEquals(JoinStep.NONE, Reborn.joinStep(0L, now));
        assertEquals(JoinStep.ATTRIBUTES_AND_CLOCK, Reborn.joinStep(now + 60_000L, now));
        assertEquals(JoinStep.DEFER_EFFECT_CLEAR, Reborn.joinStep(now, now));
        assertEquals(JoinStep.DEFER_EFFECT_CLEAR, Reborn.joinStep(now - 1L, now));
    }

    @Test
    void onlyEmiTopCountsAsTheCenteredBar() {
        assertTrue(RebornEmi.isTop(Sample.TOP));
        assertFalse(RebornEmi.isTop(Sample.LEFT));
        assertFalse(RebornEmi.isTop("TOP"));
        assertFalse(RebornEmi.isTop(null));
    }

    private enum Sample {
        TOP,
        LEFT
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
