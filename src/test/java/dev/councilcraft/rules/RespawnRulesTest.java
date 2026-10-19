package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.enchant.ForgerLevels;
import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.respawn.Reborn.JoinStep;
import dev.councilcraft.respawn.RebornEmi;
import dev.councilcraft.respawn.RebornProgress;
import dev.councilcraft.respawn.RebornStats;
import dev.councilcraft.respawn.RebornText;
import java.util.OptionalLong;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Baby respawn curve, the tooltip numbers, and the Blaze Forger level clamp. */
class RespawnRulesTest {
    @Test
    void rebornStartsSmallAndIsBackToNormalAfterSixActiveHours() {
        RebornStats start = RebornStats.at(0L);
        assertEquals(CouncilCraftRules.REBORN_SCALE, start.scale, 0.0001f);
        assertEquals(CouncilCraftRules.REBORN_HEARTS, start.hearts);
        assertEquals(CouncilCraftRules.REBORN_SPEED, start.speed, 0.0001f);
        assertEquals(CouncilCraftRules.REBORN_MELEE, start.melee, 0.0001f);
        assertEquals(CouncilCraftRules.REBORN_HUNGER, start.hunger, 0.0001f);

        RebornStats done = RebornStats.at(CouncilCraftRules.REBORN_ACTIVE_TICKS);
        assertEquals(CouncilCraftRules.VANILLA_HEARTS, done.hearts);
        assertEquals(1f, done.scale, 0.0001f);
        assertEquals(1f, done.speed, 0.0001f);
        assertEquals(1f, done.melee, 0.0001f);
        assertEquals(1f, done.hunger, 0.0001f);
        assertEquals(done.hearts, RebornStats.at(CouncilCraftRules.REBORN_ACTIVE_TICKS + 60_000L).hearts);
    }

    @Test
    void heartsStepUpAtEachQuarter() {
        long step = CouncilCraftRules.REBORN_HEART_TICKS;
        assertEquals(6 * 60 * 60 * 20 / 4, step);
        assertEquals(6, RebornStats.at(0L).hearts);
        assertEquals(6, RebornStats.at(step - 1L).hearts);
        assertEquals(7, RebornStats.at(step).hearts);
        assertEquals(8, RebornStats.at(step * 2L).hearts);
        assertEquals(9, RebornStats.at(step * 3L).hearts);
        assertEquals(10, RebornStats.at(step * 4L).hearts);
        assertEquals(CouncilCraftRules.REBORN_ACTIVE_TICKS, step * 4L);
    }

    @Test
    void sizeSpeedAndStrengthEaseInAStraightLine() {
        long halfway = CouncilCraftRules.REBORN_ACTIVE_TICKS / 2L;
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
        assertEquals(0.5f, RebornProgress.fraction(CouncilCraftRules.REBORN_ACTIVE_TICKS / 2L), 0.0001f);
        assertEquals(1f, RebornProgress.fraction(CouncilCraftRules.REBORN_ACTIVE_TICKS), 0.0001f);
        assertEquals(1f, RebornProgress.fraction(CouncilCraftRules.REBORN_ACTIVE_TICKS + 60_000L), 0.0001f);

        long halfway = CouncilCraftRules.REBORN_ACTIVE_TICKS / 2L;
        RebornStats mid = RebornStats.at(halfway);
        float scaleProgress = (mid.scale - CouncilCraftRules.REBORN_SCALE) / (1f - CouncilCraftRules.REBORN_SCALE);
        assertEquals(scaleProgress, mid.progress, 0.0001f);
        assertEquals(1.25f, mid.hunger, 0.0001f);
        assertEquals(2.5f, RebornStats.exhaustion(2f, mid.hunger), 0.0001f);
        assertEquals(1.5f, RebornStats.exhaustion(1f, RebornStats.at(0L).hunger), 0.0001f);
        assertEquals(1f, RebornStats.exhaustion(1f, RebornStats.at(CouncilCraftRules.REBORN_ACTIVE_TICKS).hunger), 0.0001f);
        assertEquals(0f, RebornStats.exhaustion(0f, CouncilCraftRules.REBORN_HUNGER), 0.0001f);
        assertEquals(-2f, RebornStats.exhaustion(-2f, CouncilCraftRules.REBORN_HUNGER), 0.0001f);
    }

    @Test
    void hoverIsTheSentenceAndTheLiveLine() throws Exception {
        String lang = Files.readString(Path.of("src/main/resources/assets/councilcraft/lang/en_us.json"));
        assertTrue(lang.contains("Grows up after 6 hours of active play."));
        assertFalse(lang.contains("on or offline"));
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
        RebornStats halfway = RebornStats.at(CouncilCraftRules.REBORN_ACTIVE_TICKS / 2L);
        assertArrayEquals(new int[] {80, 93, 85, 80}, RebornText.nowPercents(halfway));
    }

    @Test
    void progressIsSavedAndDoesNotCatchUpOffline() {
        long earned = CouncilCraftRules.REBORN_WINDOW_TICKS * 2L;
        OptionalLong saved = RebornProgress.stored(true, earned);
        assertEquals(earned, saved.getAsLong());
        assertEquals("councilcraft:reborn_ticks", CouncilCraftRules.REBORN_NBT);
        assertFalse(CouncilCraftRules.REBORN_NBT.contains("until"));

        long loaded = RebornProgress.load(saved);
        assertEquals(earned, loaded);
        for (int idle = 0; idle < 50; idle++) {
            loaded = RebornProgress.credit(loaded, false);
        }
        assertEquals(earned, loaded);
        assertEquals(RebornProgress.fraction(earned), RebornProgress.fraction(loaded), 0.0001f);

        // Dying again stores a fresh zero, not the time already earned.
        OptionalLong restarted = RebornProgress.stored(true, 0L);
        assertEquals(0L, restarted.getAsLong());
        assertEquals(0f, RebornProgress.fraction(RebornProgress.load(restarted)), 0.0001f);
        assertEquals(6, RebornStats.at(RebornProgress.load(restarted)).hearts);

        assertTrue(RebornProgress.stored(false, earned).isEmpty());
        assertEquals(-1L, RebornProgress.load(OptionalLong.empty()));
    }

    @Test
    void displayedTimeHoldsUntilAWindowCounts() {
        long ticks = 0L;
        assertEquals(CouncilCraftRules.REBORN_ACTIVE_TICKS, RebornProgress.remaining(ticks));
        ticks = RebornProgress.credit(ticks, false);
        assertEquals(CouncilCraftRules.REBORN_ACTIVE_TICKS, RebornProgress.remaining(ticks));
        ticks = RebornProgress.credit(ticks, true);
        assertEquals(
            CouncilCraftRules.REBORN_ACTIVE_TICKS - CouncilCraftRules.REBORN_WINDOW_TICKS,
            RebornProgress.remaining(ticks)
        );

        long windows = CouncilCraftRules.REBORN_ACTIVE_TICKS / CouncilCraftRules.REBORN_WINDOW_TICKS;
        long accrued = 0L;
        for (long i = 0; i < windows; i++) {
            accrued = RebornProgress.credit(accrued, true);
        }
        assertEquals(CouncilCraftRules.REBORN_ACTIVE_TICKS, accrued);
        assertEquals(0L, RebornProgress.remaining(accrued));
        assertEquals(1f, RebornProgress.fraction(accrued), 0.0001f);
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
        assertEquals(JoinStep.NONE, Reborn.joinStep(false, 0L));
        assertEquals(JoinStep.ATTRIBUTES_AND_CLOCK, Reborn.joinStep(true, 0L));
        // Logout does not expire the timer, and JOIN still does not send the effect.
        assertEquals(JoinStep.ATTRIBUTES_AND_CLOCK, Reborn.joinStep(true, 1_000L));
        assertEquals(JoinStep.DEFER_EFFECT_CLEAR, Reborn.joinStep(true, CouncilCraftRules.REBORN_ACTIVE_TICKS));
        assertEquals(JoinStep.DEFER_EFFECT_CLEAR, Reborn.joinStep(true, CouncilCraftRules.REBORN_ACTIVE_TICKS + 1L));
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
