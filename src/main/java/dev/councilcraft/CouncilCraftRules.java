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

    private CouncilCraftRules() {
    }
}
