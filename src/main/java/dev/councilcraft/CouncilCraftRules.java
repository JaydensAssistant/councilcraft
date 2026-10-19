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

    /**
     * S80 / S84 / S169. Penalty after death, shown as the Reborn status effect.
     * It lasts {@code REBORN_ACTIVE_TICKS} of active play (six hours), not wall
     * time, and it does not move while the player is idle or offline. Size is
     * Pehkui's base scale only. Hearts step up by one at each quarter of that
     * active time. Speed, strength and hunger ease in a straight line on the
     * same fraction. Dying again starts the six hours over.
     *
     * <p>The inventory tooltip's second line is these four numbers as percents.
     * Hit points are {@code REBORN_HEARTS / VANILLA_HEARTS}, so 6 of 10 hearts
     * is 60% HP. The live "Now" line uses the same four, from {@code RebornStats}.
     *
     * <p>A five-minute window is credited in full at its end, or not at all.
     * It counts when the camera moved enough, in more than one direction, and
     * the player did a real action. Chat, eating, drinking, a held button and
     * a steady macro do not count. There is no distance requirement.
     */
    public static final long REBORN_ACTIVE_TICKS = 6L * 60L * 60L * 20L;
    /** Five minutes. The activity check runs once per window, at the end. */
    public static final long REBORN_WINDOW_TICKS = 5L * 60L * 20L;
    public static final float REBORN_SCALE = 0.6f;
    public static final int REBORN_HEARTS = 6;
    public static final int VANILLA_HEARTS = 10;
    /**
     * Active ticks between heart steps. Four steps across the six hours
     * (6, 7, 8, 9, 10), so one heart every 1.5 active hours, at each 25%.
     */
    public static final long REBORN_HEART_TICKS =
        REBORN_ACTIVE_TICKS / (VANILLA_HEARTS - REBORN_HEARTS);
    public static final float REBORN_SPEED = 0.85f;
    public static final float REBORN_MELEE = 0.70f;
    /** Hunger exhaustion multiplier at rebirth. Eases to 1 with the other penalties. */
    public static final float REBORN_HUNGER = 1.5f;
    /** Total yaw plus pitch change, in degrees, required inside one window. */
    public static final float REBORN_CAMERA_DEGREES = 30f;
    /**
     * A look direction (left, right, up, down) counts once it has moved this far.
     * Less than this is not a second direction, so a one-way spin still fails.
     */
    public static final float REBORN_CAMERA_DIRECTION_DEGREES = 1f;
    /** How many of those directions have to move. One way around is not enough. */
    public static final int REBORN_CAMERA_DIRECTIONS = 2;
    /**
     * Use or attack on one target, with gaps at or below this, is one held button.
     * It does not qualify while the player is standing still. A fresh click is further apart.
     */
    public static final int REBORN_HELD_GAP_TICKS = 2;
    /** This many separate actions, at a steady beat, are a macro and do not qualify. */
    public static final int REBORN_MACRO_ACTIONS = 12;
    /** A run is a macro when every gap sits within this many ticks of the others. */
    public static final int REBORN_MACRO_TOLERANCE_TICKS = 2;
    /** Recent gestures kept per player. Enough to see a 12-action run, and no more. */
    public static final int REBORN_ACTION_RING = 16;
    /**
     * Movement below this, in blocks, still counts as standing still.
     * The window itself has no distance requirement.
     */
    public static final double REBORN_STATIONARY_BLOCKS = 0.5;
    /** Accrued active ticks. Absent means not Reborn. Zero means just respawned. */
    public static final String REBORN_NBT = "councilcraft:reborn_ticks";
    /**
     * S2C channel. First long: accrued active ticks, or -1 when Reborn is off.
     * Second long: remaining active ticks at send. The client does not add wall time.
     */
    public static final String REBORN_CHANNEL = "reborn_ticks";

    public static final UUID REBORN_HEALTH_UUID =
        UUID.fromString("c07c11a4-5aa3-4e17-8b00-0c0c11c4a003");
    public static final UUID REBORN_SPEED_UUID =
        UUID.fromString("c07c11a4-5aa3-4e17-8b00-0c0c11c4a004");
    public static final UUID REBORN_MELEE_UUID =
        UUID.fromString("c07c11a4-5aa3-4e17-8b00-0c0c11c4a005");

    /**
     * S177. Vanilla max, from {@code Enchantment.getMaxLevel}. Only these four
     * may sit one level above that, and only when a book already had that level.
     * Two books at the vanilla max do not combine into the next level.
     */
    public static final String SHARPNESS_ID = "minecraft:sharpness";
    public static final String POWER_ID = "minecraft:power";
    public static final String BLAST_PROTECTION_ID = "minecraft:blast_protection";
    public static final String PROTECTION_ID = "minecraft:protection";

    /**
     * S207 / S242. NBT boolean on the stack, not an item tag. An item tag would
     * mark every enchanted book. The grey lore line is the in-game label.
     * The CIT look for copies is POST-MVP and is not in this mod.
     */
    public static final String PRINTED_NBT = "councilcraft:printed";
    public static final String PRINTED_LORE = "Printed Copy";

    private CouncilCraftRules() {
    }
}
