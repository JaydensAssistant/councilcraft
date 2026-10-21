package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * PPM19 / PPM19a / PPM19b. Horse fall damage. No damage up to the safe distance: the horse's current
 * full-charge jump height (buff and horseshoes included) plus 2 blocks, and never less than vanilla's 6.
 * Past it, only the blocks beyond the safe distance count, at vanilla's 0.5 per block times the multiplier.
 */
public final class HorseFall {
    public static final double DAMAGE_PER_BLOCK = 0.5;
    /** Keeps a fall of exactly the safe distance from rounding up to 1 damage. */
    private static final double EPSILON = 1e-6;

    private HorseFall() {
    }

    public static double safeDistance(double jumpStrength) {
        return Math.max(CouncilCraftRules.HORSE_FALL_VANILLA_SAFE_BLOCKS,
            HorseJump.apex(jumpStrength) + CouncilCraftRules.HORSE_FALL_EXTRA_SAFE_BLOCKS);
    }

    public static int damage(float fallDistance, float damageMultiplier, double jumpStrength) {
        return Math.max(0, (int) Math.ceil(
            (fallDistance - safeDistance(jumpStrength) - EPSILON) * DAMAGE_PER_BLOCK * damageMultiplier));
    }
}
