package dev.councilcraft.rules;

/**
 * PT22 amendment. Per-baby escape cooldown for Carry On player carrying. Pure numbers, testable without a world.
 * Level L counts consecutive escapes: cooldown = 5 s x 2^(L-1), at most 30 min (5 s, 10 s, 20 s, 40 s, 80 s, ...).
 * An escape within 2x the current cooldown of the last one raises L; a later one starts again at L = 1.
 * Only escapes count (the baby frees itself); a normal Carry On place-down does not. Times are game ticks.
 */
public final class CarryEscapeRules {
    public static final int TICKS_PER_SECOND = 20;
    public static final long BASE_TICKS = 5L * TICKS_PER_SECOND;
    public static final long CAP_TICKS = 30L * 60 * TICKS_PER_SECOND;

    private CarryEscapeRules() {
    }

    /** Cooldown for escape level L (0 means no escape yet). */
    public static long cooldownTicks(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level > 20) {
            return CAP_TICKS;
        }
        return Math.min(BASE_TICKS << (level - 1), CAP_TICKS);
    }

    /** Level after an escape at {@code now}, given the level and time of the last escape. */
    public static int levelAfterEscape(int level, long lastEscape, long now) {
        if (level <= 0 || now - lastEscape > 2 * cooldownTicks(level)) {
            return 1;
        }
        return Math.min(level + 1, 64);
    }

    /** Ticks left before the baby can be picked up again (0 when free). */
    public static long ticksLeft(int level, long lastEscape, long now) {
        if (level <= 0) {
            return 0;
        }
        return Math.max(0, lastEscape + cooldownTicks(level) - now);
    }

    /** Whether the record can be forgotten: the 2x quiet window has passed, so the next escape starts at 5 s. */
    public static boolean expired(int level, long lastEscape, long now) {
        return level <= 0 || now - lastEscape > 2 * cooldownTicks(level);
    }

    /** "2m 30s", "45s", "30m": time left, rounded up to the second. */
    public static String format(long ticks) {
        long seconds = (Math.max(0, ticks) + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        long m = seconds / 60;
        long s = seconds % 60;
        if (m == 0) {
            return s + "s";
        }
        return s == 0 ? m + "m" : m + "m " + s + "s";
    }
}
