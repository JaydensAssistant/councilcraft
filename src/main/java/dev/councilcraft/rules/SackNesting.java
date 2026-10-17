package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * S236. Supplementaries 1.20-3.1.44 already returns false from {@code SackItem.canBeNested}
 * (Mojmap {@code canFitInsideContainerItems}). The mixin forces that return value so a
 * later build cannot quietly start allowing sacks inside bundles and toolboxes.
 */
public final class SackNesting {
    private SackNesting() {
    }

    public static boolean canFitInsideContainerItems() {
        return CouncilCraftRules.SACKS_CAN_FIT_INSIDE_CONTAINERS;
    }
}
