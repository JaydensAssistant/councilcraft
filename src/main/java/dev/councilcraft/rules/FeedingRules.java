package dev.councilcraft.rules;

/**
 * PM24a / PM24b / PPM34. Feeding a Reborn player. Pure numbers, testable without a world.
 * Minutes of Growth Spurt = floor(k x (hunger + saturation points)), saturation points = hunger x
 * saturation modifier x 2 (vanilla). k = 1 for Farmer's Delight meals and feast servings (item tag
 * councilcraft:reborn_feed_meals, from audit/edible-items.csv) and 2/3 for every other food (PPM34:
 * no exclusions; tiny foods just give little time). At least 1 and at most 40 minutes per feed.
 * One feed per spurt, so there is no stacking cap.
 * Growth Spurt makes credited Reborn windows count three times.
 */
public final class FeedingRules {
    public static final int MAX_MINUTES_PER_FEED = 40;
    public static final int MIN_MINUTES_PER_FEED = 1;
    /** Feeder to Reborn player, eye to eye is not needed: entity positions, in blocks. */
    public static final double MAX_DISTANCE = 3.0;
    /** "Mostly still": neither player may move this far from where the feed started. */
    public static final double MAX_DRIFT = 1.0;
    /** Growth Spurt recovery multiplier. A credited 5-minute window counts 15 minutes. */
    public static final int RECOVERY_MULTIPLIER = 3;
    public static final int TICKS_PER_MINUTE = 60 * 20;

    private FeedingRules() {
    }

    public static int minutes(boolean meal, int hunger, float saturationModifier) {
        double points = Math.max(0, hunger) + Math.max(0, hunger) * Math.max(0.0f, saturationModifier) * 2.0;
        double raw = meal ? points : points * 2.0 / 3.0;
        // The small epsilon keeps float noise (6.0 stored as 5.9999) from losing a minute.
        int minutes = (int) Math.floor(raw + 1e-4);
        return Math.max(MIN_MINUTES_PER_FEED, Math.min(MAX_MINUTES_PER_FEED, minutes));
    }

    /** Growth Spurt ticks after a feed. Feeding is one per spurt, so nothing is added to a running one. */
    public static int spurtTicks(int minutes) {
        return Math.max(0, minutes) * TICKS_PER_MINUTE;
    }

    /** PPM34. A baby can be fed again only once its Growth Spurt has run out. */
    public static boolean canFeedAgain(int remainingSpurtTicks) {
        return remainingSpurtTicks <= 0;
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
