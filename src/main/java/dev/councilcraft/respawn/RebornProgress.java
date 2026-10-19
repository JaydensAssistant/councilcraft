package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import java.util.OptionalLong;

/**
 * How far a Reborn recovery has come, from 0 at the start to 1 when it is over.
 * Every penalty reads this fraction, including hunger.
 *
 * <p>The fraction is accrued active ticks over {@link CouncilCraftRules#REBORN_ACTIVE_TICKS}.
 * Idle time and offline time are not part of it. A window credits its full five
 * minutes at the end, or nothing. Hearts use the same ticks, in quarter steps.
 */
public final class RebornProgress {
    private RebornProgress() {
    }

    public static float fraction(long activeTicks) {
        if (activeTicks <= 0L) {
            return 0f;
        }
        if (activeTicks >= CouncilCraftRules.REBORN_ACTIVE_TICKS) {
            return 1f;
        }
        return activeTicks / (float) CouncilCraftRules.REBORN_ACTIVE_TICKS;
    }

    /** Active ticks still to show. This does not shrink unless a window is credited. */
    public static long remaining(long activeTicks) {
        if (activeTicks <= 0L) {
            return CouncilCraftRules.REBORN_ACTIVE_TICKS;
        }
        if (activeTicks >= CouncilCraftRules.REBORN_ACTIVE_TICKS) {
            return 0L;
        }
        return CouncilCraftRules.REBORN_ACTIVE_TICKS - activeTicks;
    }

    /**
     * Accrued ticks after one window. A window that did not count leaves the
     * total where it was, which is also what logout does: nothing is added
     * for time that was not credited.
     */
    public static long credit(long activeTicks, boolean counted) {
        long ticks = Math.max(0L, activeTicks);
        if (!counted) {
            return ticks;
        }
        return Math.min(CouncilCraftRules.REBORN_ACTIVE_TICKS, ticks + CouncilCraftRules.REBORN_WINDOW_TICKS);
    }

    /** Value for the player NBT long, or empty when Reborn is not active. */
    public static OptionalLong stored(boolean active, long activeTicks) {
        if (!active) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(0L, activeTicks));
    }

    /**
     * Reads that NBT value back. Empty means not Reborn, so a missing key does
     * not become a finished timer and a saved count does not grow while offline.
     */
    public static long load(OptionalLong stored) {
        if (stored.isEmpty()) {
            return -1L;
        }
        return stored.getAsLong();
    }
}
