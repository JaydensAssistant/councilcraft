package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * PPM15. Horse jump height in blocks, from the 1.20.1 ridden jump arc. Pure numbers, testable without a world.
 *
 * <p>A full-charge jump starts with vertical velocity equal to the jump strength. Each tick
 * {@code travel()} applies {@code (v - 0.08) * 0.98}; on the rider's client the horse cannot "move
 * voluntarily", so {@code LivingEntity.tickMovement} applies another 0.98. Vanilla zeroes a velocity
 * under 0.003. Without the client factor the model overshot measured jumps by about 10%.
 *
 * <p>The buff maps vanilla apex heights linearly so the average horse (0.7) clears 5 blocks and the
 * best (1.0) clears 8. Horseshoes add their blocks on top, and the result is turned back into a
 * jump strength.
 */
public final class HorseJump {
    public static final double GRAVITY = 0.08;
    public static final double DRAG = 0.98;
    public static final double CLIENT_DRAG = 0.98;
    public static final double VANILLA_AVERAGE_STRENGTH = 0.7;
    public static final double VANILLA_BEST_STRENGTH = 1.0;
    /** Upper bound for the strength search. 2.0 is far above any buffed horse. */
    public static final double MAX_STRENGTH = 2.0;

    private static final double SCALE;
    private static final double OFFSET;

    static {
        double average = apex(VANILLA_AVERAGE_STRENGTH);
        double best = apex(VANILLA_BEST_STRENGTH);
        SCALE = (CouncilCraftRules.HORSE_JUMP_BEST_BLOCKS - CouncilCraftRules.HORSE_JUMP_AVERAGE_BLOCKS) / (best - average);
        OFFSET = CouncilCraftRules.HORSE_JUMP_AVERAGE_BLOCKS - SCALE * average;
    }

    private HorseJump() {
    }

    /** Highest point, in blocks, of a full-charge jump with this strength. */
    public static double apex(double strength) {
        double y = 0.0;
        double top = 0.0;
        double v = strength;
        for (int tick = 0; tick < 1000 && v > 0.0; tick++) {
            if (Math.abs(v) < 0.003) {
                // Vanilla zeroes a vertical velocity this small.
                break;
            }
            y += v;
            top = Math.max(top, y);
            v = (v - GRAVITY) * DRAG * CLIENT_DRAG;
        }
        return top;
    }

    /** The jump strength whose apex is this many blocks (binary search, capped at {@link #MAX_STRENGTH}). */
    public static double strengthFor(double blocks) {
        double lo = 0.0;
        double hi = MAX_STRENGTH;
        if (apex(hi) <= blocks) {
            return hi;
        }
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2.0;
            if (apex(mid) < blocks) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0;
    }

    /** Buffed jump height, in blocks, for a horse with this vanilla (bred) strength. */
    public static double buffedApex(double vanillaStrength) {
        return OFFSET + SCALE * apex(vanillaStrength);
    }

    /** Naturalist zebras roll their bred jump strength in 0.4 to 0.5 ({@code Zebra.generateRandomJumpStrength}). */
    public static final double ZEBRA_MIN_STRENGTH = 0.4;
    public static final double ZEBRA_MAX_STRENGTH = 0.5;
    /** Vanilla horses roll 0.4 to 1.0. */
    public static final double HORSE_MIN_STRENGTH = 0.4;

    /**
     * PPM67. A zebra's bred jump strength mapped linearly onto the horse band, so the worst zebra
     * jumps like the worst horse (0.4), the average zebra (0.45) like the average horse (0.7) and
     * the best (0.5) like the best horse (1.0). Out-of-range values (commands, other mods) clamp.
     */
    public static double zebraEquivalentStrength(double zebraStrength) {
        double clamped = Math.max(ZEBRA_MIN_STRENGTH, Math.min(ZEBRA_MAX_STRENGTH, zebraStrength));
        double t = (clamped - ZEBRA_MIN_STRENGTH) / (ZEBRA_MAX_STRENGTH - ZEBRA_MIN_STRENGTH);
        return HORSE_MIN_STRENGTH + t * (VANILLA_BEST_STRENGTH - HORSE_MIN_STRENGTH);
    }

    /** Jump strength for a horse with this vanilla strength wearing shoes that add {@code shoeBlocks}. */
    public static double strength(double vanillaStrength, float shoeBlocks) {
        return strengthFor(buffedApex(vanillaStrength) + Math.max(0.0f, shoeBlocks));
    }
}
