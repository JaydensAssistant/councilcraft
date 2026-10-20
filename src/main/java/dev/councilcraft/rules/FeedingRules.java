package dev.councilcraft.rules;

/**
 * PM24a / PM24b. Feeding a Reborn player. Pure numbers, testable without a world.
 * Minutes of Growth Spurt = floor(k x (hunger + saturation points)), saturation points = hunger x
 * saturation modifier x 2 (vanilla). k = 1 for Farmer's Delight meals and feast servings (item tag
 * councilcraft:reborn_feed_meals, from audit/edible-items.csv), 2/3 for other food with at least 4
 * hunger, 0 for food edible while full, raw or harmful food (tag councilcraft:reborn_feed_excluded,
 * duskmelon included) and snacks under 4 hunger. At most 40 minutes per feed and 60 minutes in total.
 * Growth Spurt makes credited Reborn windows count three times.
 */
public final class FeedingRules {
    public static final int MAX_MINUTES_PER_FEED = 40;
    public static final int MAX_MINUTES_TOTAL = 60;
    public static final int MIN_HUNGER_OTHER = 4;
    /** Feeder to Reborn player, eye to eye is not needed: entity positions, in blocks. */
    public static final double MAX_DISTANCE = 3.0;
    /** "Mostly still": neither player may move this far from where the feed started. */
    public static final double MAX_DRIFT = 1.0;
    /** Growth Spurt recovery multiplier. A credited 5-minute window counts 15 minutes. */
    public static final int RECOVERY_MULTIPLIER = 3;
    private static final long TICKS_PER_MINUTE = 60L * 20L;

    private FeedingRules() {
    }

    public static int minutes(boolean meal, boolean excluded, boolean alwaysEdible, int hunger, float saturationModifier) {
        if (excluded || alwaysEdible || hunger <= 0) {
            return 0;
        }
        double points = hunger + hunger * saturationModifier * 2.0;
        double raw;
        if (meal) {
            raw = points;
        } else if (hunger >= MIN_HUNGER_OTHER) {
            raw = points * 2.0 / 3.0;
        } else {
            return 0;
        }
        // The small epsilon keeps float noise (6.0 stored as 5.9999) from losing a minute.
        return (int) Math.min(MAX_MINUTES_PER_FEED, Math.floor(raw + 1e-4));
    }

    /** Growth Spurt ticks after a feed: what is left plus the new minutes, never over 60 minutes. */
    public static int stackTicks(int remainingTicks, int addMinutes) {
        long total = Math.max(0, remainingTicks) + addMinutes * TICKS_PER_MINUTE;
        return (int) Math.min(MAX_MINUTES_TOTAL * TICKS_PER_MINUTE, total);
    }

    /**
     * Extra minutes on top of a credited window's five, for the part of it spent under Growth Spurt:
     * a full window adds 10 (15 in all), half a window adds 5.
     */
    public static long extraMinutes(long growthTicks, long windowTicks, int windowMinutes) {
        if (growthTicks <= 0 || windowTicks <= 0) {
            return 0;
        }
        long ticks = Math.min(growthTicks, windowTicks);
        return (long) (RECOVERY_MULTIPLIER - 1) * windowMinutes * ticks / windowTicks;
    }
}
