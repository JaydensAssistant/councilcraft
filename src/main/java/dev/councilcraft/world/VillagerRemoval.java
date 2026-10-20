package dev.councilcraft.world;

import dev.councilcraft.CouncilCraft;
import dev.councilcraft.rules.VillagerPurge;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;

/**
 * PM42. Any villager, wandering trader, trader llama or zombie villager that is loaded into a server
 * level is discarded. The load event fires while the entity manager is adding the entity, so the
 * removal waits for the end of that server tick instead of changing the entity list mid-add.
 */
public final class VillagerRemoval {
    private static final List<Entity> PENDING = new ArrayList<>();

    private VillagerRemoval() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (VillagerPurge.purged(Registries.ENTITY_TYPE.getId(entity.getType()).toString())) {
                synchronized (PENDING) {
                    PENDING.add(entity);
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(VillagerRemoval::flush);
    }

    private static void flush(MinecraftServer server) {
        List<Entity> batch;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) {
                return;
            }
            batch = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (Entity entity : batch) {
            if (!entity.isRemoved()) {
                CouncilCraft.LOGGER.info("Removed {} at {} {} {} in {} (PM42)",
                    Registries.ENTITY_TYPE.getId(entity.getType()), entity.getBlockX(), entity.getBlockY(), entity.getBlockZ(),
                    entity.getWorld().getRegistryKey().getValue());
                entity.discard();
            }
        }
    }
}
