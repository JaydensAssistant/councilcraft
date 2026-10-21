package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 0.4.2 rules: mutant spawn ramp (PM45), coin breakdown (PM32a), villager purge (PM42). No world. */
class PostMvpRulesTest {
    private static final double EPS = 1e-9;

    @Test
    void mutantShareFollowsTheLockedRamp() {
        assertEquals(0.0, MutantSpawnRamp.share(0, 0), EPS);
        assertEquals(0.0, MutantSpawnRamp.share(2499, 0), EPS);
        assertEquals(0.0, MutantSpawnRamp.share(2500, 0), EPS);
        assertEquals(0.0, MutantSpawnRamp.share(2749, 0), EPS);
        assertEquals(0.003, MutantSpawnRamp.share(2750, 0), EPS);
        assertEquals(0.03, MutantSpawnRamp.share(5000, 0), EPS);
        assertEquals(0.054, MutantSpawnRamp.share(5000, 5000), EPS);
        assertEquals(0.072, MutantSpawnRamp.share(0, -8749), EPS);
        assertEquals(0.075, MutantSpawnRamp.share(8750, 0), EPS);
        assertEquals(0.075, MutantSpawnRamp.share(8750, 8750), EPS);
        assertEquals(0.075, MutantSpawnRamp.share(-100000, 3), EPS);
    }

    @Test
    void pickChanceGivesTheShareOfMobsNotOfPicks() {
        assertEquals(0.0, MutantSpawnRamp.pickChance(0.0, 2.5, 1.0), EPS);
        double share = 0.03;
        double other = 2.5;
        double mutant = 1.0;
        double q = MutantSpawnRamp.pickChance(share, other, mutant);
        double mutantMobs = q * mutant;
        double otherMobs = (1 - q) * other;
        assertEquals(share, mutantMobs / (mutantMobs + otherMobs), EPS);
        assertTrue(MutantSpawnRamp.isMutant("mutantmonsters:mutant_zombie"));
        assertFalse(MutantSpawnRamp.isMutant("minecraft:zombie"));
        assertFalse(MutantSpawnRamp.isMutant(null));
    }

    @Test
    void pricesShowOneMoneyTotal() {
        assertEquals("0\u00a2", CoinMoney.format(0));
        assertEquals("6\u00a2", CoinMoney.format(6));
        assertEquals("99\u00a2", CoinMoney.format(99));
        assertEquals("$1.00", CoinMoney.format(100));
        assertEquals("$1.06", CoinMoney.format(106));
        assertEquals("$2.50", CoinMoney.format(250));
        assertEquals("$123.45", CoinMoney.format(12345));
        assertEquals("$1,234.56", CoinMoney.format(123456));
        assertEquals(1234, CoinBreakdown.parseCount("1,234"));
        assertEquals(1234567, CoinBreakdown.parseCount("1\u202f234\u202f567"));
        assertEquals(-12, CoinBreakdown.parseCount("-12"));
        assertEquals(7, CoinBreakdown.parseCount(7));
        assertEquals(79, CoinBreakdown.total("3", 25, 4));
    }

    @Test
    void villagerKindsArePurged() {
        assertTrue(VillagerPurge.purged("minecraft:villager"));
        assertTrue(VillagerPurge.purged("minecraft:wandering_trader"));
        assertTrue(VillagerPurge.purged("minecraft:trader_llama"));
        assertTrue(VillagerPurge.purged("minecraft:zombie_villager"));
        assertFalse(VillagerPurge.purged("minecraft:llama"));
        assertFalse(VillagerPurge.purged("minecraft:pillager"));
        assertFalse(VillagerPurge.purged(null));
    }

    @Test
    void rebornJumpMatchesVanillaApexAndStep() {
        double vanilla = RebornJump.apex(RebornJump.JUMP_VELOCITY, 1.0);
        assertEquals(1.2522, vanilla, 1e-3);
        for (float base : new float[] {0.6f, 0.7f, 0.85f}) {
            float jump = RebornJump.jumpScale(base);
            assertTrue(jump > 1f && jump < 1f / base, "factor between 1 and 1/base");
            assertEquals(vanilla, RebornJump.apex(RebornJump.JUMP_VELOCITY * jump, base), 1e-3);
            assertEquals(0.6, 0.6 * base * RebornJump.stepScale(base), 1e-6);
        }
        assertEquals(1f, RebornJump.jumpScale(1f));
        assertEquals(1f, RebornJump.stepScale(1f));
    }

    @Test
    void exposureUploadsMustBeNormalFrames() {
        assertTrue(ExposureUploadGuard.acceptable(320, 320));
        assertTrue(ExposureUploadGuard.acceptable(2048, 2048));
        assertFalse(ExposureUploadGuard.acceptable(2049, 2049));
        assertFalse(ExposureUploadGuard.acceptable(320, 321));
        assertFalse(ExposureUploadGuard.acceptable(0, 0));
        assertFalse(ExposureUploadGuard.acceptable(-5, -5));
        assertFalse(ExposureUploadGuard.acceptable(65536, 65536));
    }

    @Test
    void vendorPaymentMakesChangeInTheLargestCoins() {
        int[] values = {1, 5, 10, 25, 50, 100};
        // 21 Dia with only Diamond Dollars: one dollar taken, 79 back as a half dollar, a quarter and 4 pennies.
        int[] taken = CoinChange.take(values, new int[] {0, 0, 0, 0, 0, 3}, 21);
        assertEquals(List.of(0, 0, 0, 0, 0, 1), java.util.Arrays.stream(taken).boxed().toList());
        int change = CoinChange.value(values, taken) - 21;
        assertEquals(79, change);
        assertEquals(List.of(4, 0, 0, 1, 1, 0), java.util.Arrays.stream(CoinChange.change(values, change)).boxed().toList());
        // Too little in total.
        assertEquals(null, CoinChange.take(values, new int[] {20, 0, 0, 0, 0, 0}, 21));
        // Small coins used before breaking a big one, never short.
        int[] mixed = CoinChange.take(values, new int[] {4, 0, 0, 0, 0, 1}, 21);
        assertTrue(CoinChange.value(values, mixed) >= 21);
        assertEquals(List.of(4, 0, 0, 0, 0, 1), java.util.Arrays.stream(mixed).boxed().toList());
        int[] spread = CoinChange.take(values, new int[] {30, 1, 1, 0, 0, 0}, 21);
        assertEquals(21, CoinChange.value(values, spread));
    }

    @Test
    void feedingMinutesFollowTheLockedRates() {
        // FD feast serving / meal, k = 1: roast chicken 14 + 21 = 35.
        assertEquals(35, FeedingRules.minutes(true, 14, 0.75f));
        // Other food, k = 2/3: cooked porkchop 8 + 12.8 = 20.8 -> 13.
        assertEquals(13, FeedingRules.minutes(false, 8, 0.8f));
        // Bread 5 + 6 = 11 -> 7.
        assertEquals(7, FeedingRules.minutes(false, 5, 0.6f));
        // PPM34: snacks and always-edible food count too (3 + 1.8 = 4.8 -> 3; golden apple 4 + 9.6 -> 9).
        assertEquals(3, FeedingRules.minutes(false, 3, 0.3f));
        assertEquals(9, FeedingRules.minutes(false, 4, 1.2f));
        // Tiny food still gives at least a minute; cap 40 per feed.
        assertEquals(1, FeedingRules.minutes(false, 1, 0.1f));
        assertEquals(1, FeedingRules.minutes(false, 0, 0.0f));
        assertEquals(40, FeedingRules.minutes(true, 20, 1.0f));
        // One feed per spurt: no stacking.
        assertEquals(35 * 1200, FeedingRules.spurtTicks(35));
        assertTrue(FeedingRules.canFeedAgain(0));
        assertFalse(FeedingRules.canFeedAgain(1));
        // Full window under Growth Spurt: 5 + 10 = 15 minutes; half a window: +5.
        assertEquals(10, FeedingRules.extraMinutes(6000, 6000, 5));
        assertEquals(5, FeedingRules.extraMinutes(3000, 6000, 5));
        assertEquals(0, FeedingRules.extraMinutes(0, 6000, 5));
    }
}
