package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * PPM18. Gold horseshoe loot is "definitely lower". Horseshoes 1.0.3 injects a gold-only pool
 * from code into nether-bridge chests (1-2 rolls) and all four bastion chests (2-3 rolls).
 * {@code HorseshoeLootMixin} turns that pool into one roll at the chance below. The mixed
 * iron/gold/diamond pools in desert pyramid and end city chests are unchanged.
 */
public final class GoldHorseshoeLoot {
    public static final String NETHER_BRIDGE = "minecraft:chests/nether_bridge";

    private GoldHorseshoeLoot() {
    }

    /** One-roll chance for the gold pool in this loot table, or NaN to leave the pool as it is. */
    public static float chance(String lootTableId) {
        if (NETHER_BRIDGE.equals(lootTableId)) {
            return CouncilCraftRules.GOLD_HORSESHOE_FORTRESS_CHANCE;
        }
        switch (lootTableId) {
            case "minecraft:chests/bastion_bridge":
            case "minecraft:chests/bastion_hoglin_stable":
            case "minecraft:chests/bastion_other":
            case "minecraft:chests/bastion_treasure":
                return CouncilCraftRules.GOLD_HORSESHOE_BASTION_CHANCE;
            default:
                return Float.NaN;
        }
    }
}
