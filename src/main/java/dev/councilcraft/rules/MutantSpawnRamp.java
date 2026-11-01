package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * PM45 / CC-159. Share of natural hostile spawns that are Mutant Monsters, by straight-line distance
 * from 0,0. 0 within 2,500 blocks; then +0.000012 share per block beyond that, continuous, with no
 * cap (~3% at 5,000, 7.5% at 8,750, and higher farther out).
 *
 * <p>A spawn attempt picks one entry, then spawns a pack of that entry. The share is of mobs, so the
 * chance to pick the mutant pool is corrected for the mean pack size of each pool.
 */
public final class MutantSpawnRamp {
    private MutantSpawnRamp() {
    }

    public static double share(double x, double z) {
        double distance = Math.sqrt(x * x + z * z);
        if (distance <= CouncilCraftRules.MUTANT_FREE_RADIUS) {
            return 0.0;
        }
        return (distance - CouncilCraftRules.MUTANT_FREE_RADIUS) * CouncilCraftRules.MUTANT_PER_BLOCK_SHARE;
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
