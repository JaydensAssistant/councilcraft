package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;
import java.math.BigDecimal;

/**
 * S230 / S242 / PPM4 / PPM14 / PPM15. Maps a horseshoe item id to the stats this pack uses.
 * Iron and diamond speed are half of what Horseshoes 1.0.3 hard-codes (0.1 and 0.15).
 * Gold is back (PPM4) with its own speed and armor. Netherite is our item. Anything else
 * keeps the value the item itself reports.
 *
 * <p>PPM12. The horseshoe tooltips read the same table, so they cannot drift from the stats.
 */
public final class HorseshoeTiers {
    private static final Tier IRON = new Tier(
        CouncilCraftRules.IRON_HORSESHOE_SPEED, CouncilCraftRules.IRON_HORSESHOE_ARMOR,
        CouncilCraftRules.IRON_HORSESHOE_STEP, 0.0f, CouncilCraftRules.IRON_HORSESHOE_JUMP);
    private static final Tier GOLD = new Tier(
        CouncilCraftRules.GOLD_HORSESHOE_SPEED, CouncilCraftRules.GOLD_HORSESHOE_ARMOR,
        CouncilCraftRules.GOLD_HORSESHOE_STEP, 0.0f, CouncilCraftRules.GOLD_HORSESHOE_JUMP);
    private static final Tier DIAMOND = new Tier(
        CouncilCraftRules.DIAMOND_HORSESHOE_SPEED, CouncilCraftRules.DIAMOND_HORSESHOE_ARMOR,
        CouncilCraftRules.DIAMOND_HORSESHOE_STEP, 0.0f, CouncilCraftRules.DIAMOND_HORSESHOE_JUMP);
    private static final Tier NETHERITE = new Tier(
        CouncilCraftRules.NETHERITE_HORSESHOE_SPEED, CouncilCraftRules.NETHERITE_HORSESHOE_ARMOR,
        CouncilCraftRules.NETHERITE_HORSESHOE_STEP, CouncilCraftRules.NETHERITE_HORSESHOE_KNOCKBACK_RESISTANCE,
        CouncilCraftRules.NETHERITE_HORSESHOE_JUMP);

    private HorseshoeTiers() {
    }

    /** The tier for this item id, or null when it is not a horseshoe we change. */
    public static Tier tier(String itemId) {
        if (CouncilCraftRules.IRON_HORSESHOES_ID.equals(itemId)) {
            return IRON;
        }
        if (CouncilCraftRules.GOLD_HORSESHOE_ID.equals(itemId)) {
            return GOLD;
        }
        if (CouncilCraftRules.DIAMOND_HORSESHOES_ID.equals(itemId)) {
            return DIAMOND;
        }
        if (CouncilCraftRules.NETHERITE_HORSESHOES_ID.equals(itemId)) {
            return NETHERITE;
        }
        return null;
    }

    public static float speed(String itemId, float itemValue) {
        Tier tier = tier(itemId);
        return tier == null ? itemValue : tier.speed();
    }

    public static float armor(String itemId, float itemValue) {
        Tier tier = tier(itemId);
        return tier == null ? itemValue : tier.armor();
    }

    /** Blocks this shoe lets a ridden horse step up. Zero means "not a tier we change". */
    public static float stepHeight(String itemId) {
        Tier tier = tier(itemId);
        return tier == null ? 0.0f : tier.stepHeight();
    }

    /** PPM15. Blocks of jump height this shoe adds. Zero for anything else. */
    public static float jumpBlocks(String itemId) {
        Tier tier = tier(itemId);
        return tier == null ? 0.0f : tier.jumpBlocks();
    }

    public static float knockbackResistance(String itemId) {
        Tier tier = tier(itemId);
        return tier == null ? 0.0f : tier.knockbackResistance();
    }

    /** Tooltip number without float noise or trailing zeros: 0.0625, 4.5, 3.1, 2. */
    public static String plain(float value) {
        return new BigDecimal(Float.toString(value)).stripTrailingZeros().toPlainString();
    }

    /**
     * PPM59. Minecraft Wiki horse factor: one point of movement_speed is about 42.16 blocks per second on flat ground.
     * Horseshoes adds the tier speed as an ADDITION modifier, so the bonus is linear in it.
     */
    public static final double HORSE_BLOCKS_PER_SECOND_PER_SPEED = 42.16;

    /** PPM59. The tier's speed bonus in blocks per second, one decimal: 0.05 -> "2.1". */
    public static String blocksPerSecond(float speed) {
        return new BigDecimal(speed * HORSE_BLOCKS_PER_SECOND_PER_SPEED).setScale(1, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** PPM20. The singular lang key at exactly 1 ("+1 block"), otherwise the plural one. */
    public static String blocksKey(String key, float value) {
        return value == 1.0f ? key + ".one" : key;
    }

    /** One horseshoe tier. Step and jump are in blocks. */
    public record Tier(float speed, float armor, float stepHeight, float knockbackResistance, float jumpBlocks) {
    }
}
