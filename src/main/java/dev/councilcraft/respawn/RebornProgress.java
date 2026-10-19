package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import java.util.OptionalLong;

/**
 * How far a Reborn recovery has come, from 0 at the start to 1 when it is over.
 * Every penalty reads this fraction, including hearts and hunger.
 *
 * <p>The fraction is accrued active minutes over {@link CouncilCraftRules#REBORN_ACTIVE_MINUTES}.
 * Idle time and offline time are not part of it. A window credits its full five
 * minutes at the end, or nothing. A partial window is not stored.
 */
public final class RebornProgress {
    private RebornProgress() {
    }

    public static float fraction(long activeMinutes) {
        if (activeMinutes <= 0L) {
            return 0f;
        }
        if (activeMinutes >= CouncilCraftRules.REBORN_ACTIVE_MINUTES) {
            return 1f;
        }
        return activeMinutes / (float) CouncilCraftRules.REBORN_ACTIVE_MINUTES;
    }

    /** Active minutes still to show. This does not shrink unless a window is credited. */
    public static long remaining(long activeMinutes) {
        if (activeMinutes <= 0L) {
            return CouncilCraftRules.REBORN_ACTIVE_MINUTES;
        }
        if (activeMinutes >= CouncilCraftRules.REBORN_ACTIVE_MINUTES) {
            return 0L;
        }
        return CouncilCraftRules.REBORN_ACTIVE_MINUTES - activeMinutes;
    }

    /** Effect duration, in ticks, for the remaining active minutes. */
    public static long durationTicks(long activeMinutes) {
        return remaining(activeMinutes) * 60L * 20L;
    }

    /**
     * Accrued minutes after one window. A window that did not count leaves the
     * total where it was, which is also what logout does: nothing is added
     * for time that was not credited.
     */
    public static long credit(long activeMinutes, boolean counted) {
        long minutes = Math.max(0L, activeMinutes);
        if (!counted) {
            return minutes;
        }
        return Math.min(
            CouncilCraftRules.REBORN_ACTIVE_MINUTES,
            minutes + CouncilCraftRules.REBORN_WINDOW_MINUTES
        );
    }

    /** Value for the player NBT int, or empty when Reborn is not active. */
    public static OptionalLong stored(boolean active, long activeMinutes) {
        if (!active) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(0L, activeMinutes));
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
