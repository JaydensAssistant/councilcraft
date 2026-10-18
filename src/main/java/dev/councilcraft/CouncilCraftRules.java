package dev.councilcraft;

import java.util.UUID;

/**
 * Every CouncilCraft number and id lives here. Mixins and items read this class;
 * they do not hard-code their own copies.
 *
 * <p>Spec ids (S123, S226, ...) refer to SPEC.md. A line tagged SUPERSEDED there
 * is not implemented; the newer entry named in the tag is.
 */
public final class CouncilCraftRules {
    public static final String MOD_ID = "councilcraft";

    /** Login-query channel. A client that does not answer it is refused (S244 G5). */
    public static final String HANDSHAKE_CHANNEL = "handshake";

    /**
     * S226. Vanilla multiplies the riptide vector by {@code 3.0 * (1 + level) / 4}.
     * Half launch strength means that {@code 3.0} becomes {@code 1.5}.
     */
    public static final float RIPTIDE_VANILLA_COEFFICIENT = 3.0f;
    public static final float RIPTIDE_STRENGTH = 0.5f;

    /** S228. Depth Strider's level while Dolphin's Grace is active. */
    public static final int DEPTH_STRIDER_UNDER_DOLPHINS_GRACE = 0;

    /** S244. Anvil repair ingredient added on top of the mod's own leather check. */
    public static final String PARAGLIDER_REPAIR_ITEM = "minecraft:phantom_membrane";

    /**
     * S233 / S236. Sacks must report that they cannot nest inside bundles,
     * Create toolboxes, or any other container that honours this flag.
     */
    public static final boolean SACKS_CAN_FIT_INSIDE_CONTAINERS = false;

    /**
     * S245. Horseshoes 1.0.3 injects this item from code (there is no loot JSON).
     * Any pool entry with this id is removed. A pool that contained only gold is
     * dropped entirely, so bastion and nether-bridge chests gain no extra pool.
     */
    public static final String GOLD_HORSESHOE_ID = "horseshoes:gold_horseshoes";

    /**
     * S230 / S242. Speed is added to the horse's movement attribute (operation ADDITION),
     * the same way Horseshoes 1.0.3 does. Armor matches the mod's own iron and diamond
     * values. Step height is applied by us, and only while the horse is ridden.
     * Knockback resistance is netherite only.
     */
    public static final String IRON_HORSESHOES_ID = "horseshoes:iron_horseshoes";
    public static final String DIAMOND_HORSESHOES_ID = "horseshoes:diamond_horseshoes";
    public static final String NETHERITE_HORSESHOES_ID = "councilcraft:netherite_horseshoes";

    public static final float IRON_HORSESHOE_SPEED = 0.05f;
    public static final float IRON_HORSESHOE_ARMOR = 3.5f;
    public static final float IRON_HORSESHOE_STEP = 2.1f;

    public static final float DIAMOND_HORSESHOE_SPEED = 0.075f;
    public static final float DIAMOND_HORSESHOE_ARMOR = 5.5f;
    public static final float DIAMOND_HORSESHOE_STEP = 2.6f;

    public static final float NETHERITE_HORSESHOE_SPEED = 0.1f;
    public static final float NETHERITE_HORSESHOE_ARMOR = 7.0f;
    public static final float NETHERITE_HORSESHOE_KNOCKBACK_RESISTANCE = 0.1f;
    public static final float NETHERITE_HORSESHOE_STEP = 3.1f;

    /**
     * S221 / S244. Attribute units, not blocks per tick. A top-roll horse in netherite
     * shoes is about 0.4375. Anything above this (Speed, Soul Speed, a camel dash) is
     * cut back to this. Horses, zebras, donkeys, mules, camels and llamas are all
     * AbstractHorse.
     */
    public static final float HORSE_SPEED_CAP = 0.44f;

    /**
     * Horseshoes 1.0.3 puts the shoes in inventory slot 2 when the animal has an armor
     * slot (horse) and slot 1 otherwise (donkey, mule, skeleton horse, zombie horse).
     */
    public static final int HORSESHOE_SLOT_WITH_ARMOR = 2;
    public static final int HORSESHOE_SLOT_WITHOUT_ARMOR = 1;

    /** S230. Own modifier, separate from the Horseshoes speed and armor UUIDs. */
    public static final UUID NETHERITE_HORSESHOE_KNOCKBACK_UUID =
        UUID.fromString("c07c11a4-5aa3-4e17-8b00-0c0c11c4a009");

    /** S234. Datapack tag. A datapack can add or remove items without a code change. */
    public static final String TOOLBOX_TAG_PATH = "toolbox_contents";

    /**
     * S123. Numismatics 1.1.0 ships SPUR 1, BEVEL 8, SPROCKET 16, COG 64, CROWN 512, SUN 4096.
     * One unit is one diamond. Names are the enum constants.
     */
    public static final int SPUR_VALUE = 1;
    public static final int BEVEL_VALUE = 5;
    public static final int SPROCKET_VALUE = 10;
    public static final int COG_VALUE = 20;
    public static final int CROWN_VALUE = 50;
    public static final int SUN_VALUE = 100;

    /**
     * Sprocket (10 diamonds) and cog (20 diamonds) are the only mint recipes with
     * nine or more loose diamonds. The press also accepts the vanilla 3x3 diamond
     * block recipe, and extra items do not stop that recipe matching. These two
     * molds must beat it no matter which item was inserted first.
     */
    public static final String DIAMOND_BLOCK_ID = "minecraft:diamond_block";
    public static final String SPROCKET_MOLD_ID = "councilcraft:sprocket_mold";
    public static final String COG_MOLD_ID = "councilcraft:cog_mold";

    /**
     * S121. One patrol in four gets an evoker. {@code nextInt(4) == 0} is exactly 25%.
     * Rolled once, when the captain spawns, not once per member.
     */
    public static final int PATROL_EVOKER_SIDES = 4;

    private CouncilCraftRules() {
    }
}
