package dev.councilcraft;

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

    private CouncilCraftRules() {
    }
}
