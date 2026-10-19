package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * S230 / S242. Maps a horseshoe item id to the stats this pack uses.
 * Iron and diamond speed are half of what Horseshoes 1.0.3 hard-codes
 * (0.1 and 0.15). Their armor already matches the spec, so only speed is
 * replaced. Netherite is our item. Anything else, including a gold horseshoe
 * produced with commands, keeps the value the item itself reports.
 */
public final class HorseshoeTiers {
    private HorseshoeTiers() {
    }

    public static float speed(String itemId, float itemValue) {
        if (CouncilCraftRules.IRON_HORSESHOES_ID.equals(itemId)) {
            return CouncilCraftRules.IRON_HORSESHOE_SPEED;
        }
        if (CouncilCraftRules.DIAMOND_HORSESHOES_ID.equals(itemId)) {
            return CouncilCraftRules.DIAMOND_HORSESHOE_SPEED;
        }
        if (CouncilCraftRules.NETHERITE_HORSESHOES_ID.equals(itemId)) {
            return CouncilCraftRules.NETHERITE_HORSESHOE_SPEED;
        }
        return itemValue;
    }

    /** Blocks this shoe lets a ridden horse step up. Zero means "not a tier we change". */
    public static float stepHeight(String itemId) {
        if (CouncilCraftRules.IRON_HORSESHOES_ID.equals(itemId)) {
            return CouncilCraftRules.IRON_HORSESHOE_STEP;
        }
        if (CouncilCraftRules.DIAMOND_HORSESHOES_ID.equals(itemId)) {
            return CouncilCraftRules.DIAMOND_HORSESHOE_STEP;
        }
        if (CouncilCraftRules.NETHERITE_HORSESHOES_ID.equals(itemId)) {
            return CouncilCraftRules.NETHERITE_HORSESHOE_STEP;
        }
        return 0.0f;
    }

    public static float knockbackResistance(String itemId) {
        if (CouncilCraftRules.NETHERITE_HORSESHOES_ID.equals(itemId)) {
            return CouncilCraftRules.NETHERITE_HORSESHOE_KNOCKBACK_RESISTANCE;
        }
        return 0.0f;
    }
}
