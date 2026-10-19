package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/** S226. Pure half of the vanilla riptide coefficient, so the rule can be tested without the game. */
public final class RiptideLaunch {
    private RiptideLaunch() {
    }

    /** @param vanillaCoefficient the {@code 3.0f} literal in {@code TridentItem.onStoppedUsing} */
    public static float halvedCoefficient(float vanillaCoefficient) {
        return vanillaCoefficient * CouncilCraftRules.RIPTIDE_STRENGTH;
    }

    /**
     * Vanilla launch speed added by riptide, in blocks per tick, before friction.
     * Level 1, 2, 3 → 1.5, 2.25, 3.0. Ours is half of that.
     */
    public static float launchBlocksPerTick(int riptideLevel, float coefficient) {
        return coefficient * ((1.0f + riptideLevel) / 4.0f);
    }
}
