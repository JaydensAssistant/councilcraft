package dev.councilcraft.rules;

import java.util.Set;

/**
 * S81 / S94 / S135 / PM42. The pack has no villagers. KubeJS cancels fresh spawns; this list is also
 * removed whenever one of these is loaded into a level (structure-template entities placed at worldgen,
 * entities already saved in a world, Carry On placements).
 */
public final class VillagerPurge {
    public static final Set<String> PURGED = Set.of(
        "minecraft:villager",
        "minecraft:wandering_trader",
        "minecraft:trader_llama",
        "minecraft:zombie_villager"
    );

    private VillagerPurge() {
    }

    public static boolean purged(String entityTypeId) {
        return entityTypeId != null && PURGED.contains(entityTypeId);
    }
}
