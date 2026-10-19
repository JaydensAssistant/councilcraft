package dev.councilcraft.enchant;

import dev.councilcraft.CouncilCraftRules;

/**
 * S177. What a Blaze Forger result level is allowed to be.
 *
 * <p>{@code existing} and {@code incoming} are the levels before the merge.
 * {@code result} is what the forger computed, including its own extension.
 * Two inputs that are both at or under the vanilla max cannot produce a level
 * above that max. An input that is already above the max can keep it, and only
 * Sharpness, Power, Blast Protection and Protection may be one above the max.
 */
public final class ForgerLevels {
    private ForgerLevels() {
    }

    public static boolean mayExceedVanillaMax(String enchantmentId) {
        return CouncilCraftRules.SHARPNESS_ID.equals(enchantmentId)
            || CouncilCraftRules.POWER_ID.equals(enchantmentId)
            || CouncilCraftRules.BLAST_PROTECTION_ID.equals(enchantmentId)
            || CouncilCraftRules.PROTECTION_ID.equals(enchantmentId);
    }

    public static int clamp(boolean mayExceed, int vanillaMax, int existing, int incoming, int result) {
        int ceiling = mayExceed ? vanillaMax + 1 : vanillaMax;
        if (existing <= vanillaMax && incoming <= vanillaMax) {
            return Math.min(result, vanillaMax);
        }
        return Math.min(result, ceiling);
    }
}
