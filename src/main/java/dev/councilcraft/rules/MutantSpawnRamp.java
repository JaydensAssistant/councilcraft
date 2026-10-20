package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * PM45. Share of natural hostile spawns that are Mutant Monsters, by straight-line distance from 0,0.
 * 0 within 2,500 blocks; then +0.3% for every full 250 blocks past 2,500; capped at 7.5% (reached at
 * 8,750 and kept beyond it). At the starting +-5k border that is 3% at the middle of an edge and 5.4%
 * in the corners.
 *
 * <p>A spawn attempt picks one entry, then spawns a pack of that entry. The share is of mobs, so the
 * chance to pick the mutant pool is corrected for the mean pack size of each pool.
 */
public final class MutantSpawnRamp {
    private MutantSpawnRamp() {
    }

    public static double share(double x, double z) {
        double distance = Math.sqrt(x * x + z * z);
        if (distance < CouncilCraftRules.MUTANT_FREE_RADIUS) {
            return 0.0;
        }
        long steps = (long) Math.floor((distance - CouncilCraftRules.MUTANT_FREE_RADIUS) / CouncilCraftRules.MUTANT_STEP_BLOCKS);
        return Math.min(CouncilCraftRules.MUTANT_MAX_SHARE, steps * CouncilCraftRules.MUTANT_STEP_SHARE);
    }

    /**
     * Chance that one spawn attempt picks from the mutant pool, so that mutants are {@code share} of the
     * mobs spawned. {@code otherPack} and {@code mutantPack} are the weighted mean pack sizes.
     */
    public static double pickChance(double share, double otherPack, double mutantPack) {
        if (share <= 0.0) {
            return 0.0;
        }
        if (share >= 1.0) {
            return 1.0;
        }
        double a = share * otherPack;
        double b = (1.0 - share) * mutantPack;
        return a + b <= 0.0 ? 0.0 : a / (a + b);
    }

    public static boolean isMutant(String entityTypeId) {
        return entityTypeId != null && entityTypeId.startsWith(CouncilCraftRules.MUTANT_NAMESPACE + ":");
    }
}
