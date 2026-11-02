package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * CC-173A. Improved Mobs phantom/guardian rider chance by distance from 0,0 (world spawn
 * for this pack). 0 within 249 blocks; then {@code floor(dist/250) * (0.075/28)} with no
 * cap (~2.7% at 2,500, ~5.4% at 5,000, 7.5% at 7,000, ~13.1% at 12,374).
 *
 * <p>Applied server-side by {@code RiderChanceMixin} over IM's flat flyAIChance /
 * guardianAIChance reads in EventCalls.onEntityLoad.
 */
public final class RiderChance {
    private RiderChance() {
    }

    /** Chance in [0, ∞) for a mob at (x, z) to get a phantom or guardian rider roll. */
    public static double chance(double x, double z) {
        double dist = Math.sqrt(x * x + z * z);
        int steps = (int) Math.floor(dist / CouncilCraftRules.RIDER_STEP_BLOCKS);
        return steps * CouncilCraftRules.RIDER_PER_STEP;
    }
}
