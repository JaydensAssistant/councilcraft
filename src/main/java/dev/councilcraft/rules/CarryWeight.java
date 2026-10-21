package dev.councilcraft.rules;

/**
 * PPM35. Carry On's weight level maps straight to Supplementaries' overencumbered amplifier, capped at 3:
 * 0 no sprint, 1 also no jumping up a block, 2 -15% walk, 3 -30% walk. Pure numbers.
 */
public final class CarryWeight {
    /** Long enough that the icon does not flicker between refreshes; cleared on drop. */
    public static final int DURATION_TICKS = 30 * 20;
    /** Re-apply at most this often while the level and effect are unchanged. */
    public static final int REFRESH_TICKS = 5 * 20;
    public static final int MAX_AMPLIFIER = 3;

    private CarryWeight() {
    }

    public static int amplifier(int carryOnLevel) {
        return Math.max(0, Math.min(MAX_AMPLIFIER, carryOnLevel));
    }

    /** Whether Carry On's per-tick call should refresh the effect now. */
    public static boolean shouldApply(int amplifier, int lastAmplifier, long ticksSinceLast, boolean hasEffect) {
        return amplifier != lastAmplifier || !hasEffect || ticksSinceLast >= REFRESH_TICKS || ticksSinceLast < 0;
    }
}
