package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import java.util.OptionalLong;

/**
 * How far a Reborn recovery has come, from 0 at the start to 1 when it is over.
 * Size, speed, strength and hunger read this fraction. Hearts step on their own.
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

    /** Remaining active time, in ticks. */
    public static long durationTicks(long activeMinutes) {
        return remaining(activeMinutes) * 60L * 20L;
    }

    /**
     * Duration written onto the effect at a window boundary. While recovery is
     * unfinished this stays at least one window plus a small pad, so the icon
     * cannot expire before the next rewrite.
     */
    public static long displayTicks(long activeMinutes) {
        long remaining = durationTicks(activeMinutes);
        if (remaining <= 0L) {
            return 0L;
        }
        long floor = CouncilCraftRules.REBORN_WINDOW_TICKS + CouncilCraftRules.REBORN_DURATION_PAD_TICKS;
        return Math.max(remaining, floor);
    }

    /**
     * Minutes to load. A saved count wins. Legacy data with no minute count
     * starts a fresh Reborn at zero, so an old effect is not left stuck.
     * Neither means not Reborn.
     */
    public static long adopt(boolean hasMinutes, long minutes, boolean legacy) {
        if (hasMinutes) {
            return Math.max(0L, minutes);
        }
        if (legacy) {
            return 0L;
        }
        return -1L;
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
