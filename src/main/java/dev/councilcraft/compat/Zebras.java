package dev.councilcraft.compat;

import dev.councilcraft.CouncilCraftRules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;

/** PPPM3. Naturalist zebras, identified by entity type id so nothing loads Naturalist classes. */
public final class Zebras {
    private Zebras() {
    }

    public static boolean isZebra(Entity entity) {
        return entity != null && isZebra(entity.getType());
    }

    public static boolean isZebra(EntityType<?> type) {
        return CouncilCraftRules.ZEBRA_ID.equals(Registries.ENTITY_TYPE.getId(type).toString());
    }
}
