package dev.councilcraft.advancement;

import dev.councilcraft.CouncilCraftRules;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * PT42. Registers {@code councilcraft:milestone} and the once-a-second poll for Here Be Mutants and Double Saddle
 * (and Shared Horizons when Surveyor adds its check). The criterion ignores players who already have the advancement.
 */
public final class Milestones {
    public static final MilestoneCriterion MILESTONE = Criteria.register(new MilestoneCriterion());

    /** Horse-family mounts that seat two players (horse, donkey, mule, skeleton and zombie horse). */
    public static final TagKey<EntityType<?>> DOUBLE_SADDLE_MOUNTS =
        TagKey.of(RegistryKeys.ENTITY_TYPE, new Identifier(CouncilCraftRules.MOD_ID, "double_saddle"));

    private static Consumer<ServerPlayerEntity> extraPoll = player -> { };

    private Milestones() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(Milestones::tick);
    }

    /** Adds a per-player check to the poll, for compat classes that load only when their mod is present. */
    public static void addPoll(Consumer<ServerPlayerEntity> poll) {
        extraPoll = extraPoll.andThen(poll);
    }

    public static void grant(ServerPlayerEntity player, String milestone) {
        MILESTONE.trigger(player, milestone);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % MilestoneRules.POLL_TICKS != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator()) {
                continue;
            }
            if (player.getWorld().getRegistryKey() == World.OVERWORLD
                && MilestoneRules.pastMutantLine(player.getX(), player.getZ())) {
                grant(player, MilestoneRules.HERE_BE_MUTANTS);
            }
            Entity vehicle = player.getVehicle();
            if (vehicle != null && vehicle.getType().isIn(DOUBLE_SADDLE_MOUNTS)) {
                int riders = 0;
                for (Entity passenger : vehicle.getPassengerList()) {
                    if (passenger instanceof ServerPlayerEntity) {
                        riders++;
                    }
                }
                if (MilestoneRules.doubleSaddle(riders)) {
                    grant(player, MilestoneRules.DOUBLE_SADDLE);
                }
            }
            extraPoll.accept(player);
        }
    }
}
