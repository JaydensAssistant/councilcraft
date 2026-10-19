package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * How far a Reborn recovery has come, from 0 at the start to 1 when it is over.
 * Every penalty reads this fraction, including hunger.
 *
 * <p>Today the fraction is wall-clock time since respawn, offline included,
 * over {@link CouncilCraftRules#REBORN_MILLIS}. A different model replaces
 * {@link #fraction(long)} and leaves the penalties alone.
 */
public final class RebornProgress {
    private RebornProgress() {
    }

    /** Millis since recovery started, from the saved deadline. Never negative. */
    public static long elapsed(long until, long now) {
        if (until <= 0L) {
            return 0L;
        }
        long start = until - CouncilCraftRules.REBORN_MILLIS;
        return Math.max(0L, now - start);
    }

    public static float fraction(long elapsedMillis) {
        if (elapsedMillis <= 0L) {
            return 0f;
        }
        if (elapsedMillis >= CouncilCraftRules.REBORN_MILLIS) {
            return 1f;
        }
        return elapsedMillis / (float) CouncilCraftRules.REBORN_MILLIS;
    }
}
