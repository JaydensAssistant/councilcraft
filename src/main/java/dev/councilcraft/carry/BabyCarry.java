package dev.councilcraft.carry;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.rules.CarryEscapeRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.carry.PlacementHandler;

/**
 * PT22 and its amendment. Carry On player carrying is on (pickupPlayers=true) but only adults may pick up
 * Reborn babies: this server-side UseEntityCallback runs in a phase before Carry On's (default phase) and
 * refuses (FAIL) when the carrier is Reborn, the target is not Reborn, or the baby is in its escape cooldown
 * (action bar: "Can't pick them up for 2m 30s"). It only acts when Carry On would try a pickup (empty hands,
 * carry key held, not carrying). Escapes are counted in {@code CarryEscapeMixin} (the baby sneaks free).
 *
 * <p>Edge paths: a carrier who logs out or dies sets the baby down first (Carry On's placeCarried). A baby who
 * logs out or dies while carried is dismounted first, because vanilla PlayerManager.remove would otherwise
 * mark the carrier (the root vehicle of a player rider) as removed. A carrier whose Carry On data says
 * "carrying a player" with no player riding (after a crash) is cleared.
 */
public final class BabyCarry {
    public static final String COOLDOWN = "councilcraft.carry.cooldown";
    private static final Identifier BEFORE_CARRY_ON = new Identifier(CouncilCraftRules.MOD_ID, "before_carry_on");

    private static volatile CarryEscapeState state;

    private BabyCarry() {
    }

    public static void register() {
        UseEntityCallback.EVENT.addPhaseOrdering(BEFORE_CARRY_ON, Event.DEFAULT_PHASE);
        UseEntityCallback.EVENT.register(BEFORE_CARRY_ON, (player, world, hand, entity, hit) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity carrier)
                || !(entity instanceof ServerPlayerEntity baby)) {
                return ActionResult.PASS;
            }
            return pickup(carrier, baby);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> state = server.getOverworld().getPersistentStateManager()
            .getOrCreate(CarryEscapeState::fromNbt, CarryEscapeState::new, CarryEscapeState.ID));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> state = null);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> release(handler.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                release(player);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(BabyCarry::tick);
    }

    private static ActionResult pickup(ServerPlayerEntity carrier, ServerPlayerEntity baby) {
        if (!carrier.getMainHandStack().isEmpty() || !carrier.getOffHandStack().isEmpty()) {
            return ActionResult.PASS;
        }
        CarryOnData carry = CarryOnDataManager.getCarryData(carrier);
        if (carry.isCarrying() || !carry.isKeyPressed()) {
            return ActionResult.PASS;
        }
        // Kids can't carry kids; adults can carry Reborn players only.
        if (Reborn.running(carrier) || !Reborn.running(baby)) {
            return ActionResult.FAIL;
        }
        long left = ticksLeft(carrier.getServer(), baby);
        if (left > 0) {
            carrier.sendMessage(Text.translatable(COOLDOWN, CarryEscapeRules.format(left)), true);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private static long ticksLeft(MinecraftServer server, ServerPlayerEntity baby) {
        CarryEscapeState s = state;
        return s == null || server == null ? 0 : s.ticksLeft(baby.getUuid(), server.getOverworld().getTime());
    }

    /** Whether this player is riding a carrier who carries a player with Carry On. */
    public static boolean carried(PlayerEntity player) {
        Entity vehicle = player.getVehicle();
        return vehicle instanceof ServerPlayerEntity carrier
            && CarryOnDataManager.getCarryData(carrier).isCarrying(CarryOnData.CarryType.PLAYER);
    }

    /** Called when a carried player sneaks free (vanilla dismount in PlayerEntity.tickRiding). */
    public static void escaped(ServerPlayerEntity baby) {
        CarryEscapeState s = state;
        MinecraftServer server = baby.getServer();
        if (s != null && server != null) {
            s.escape(baby.getUuid(), server.getOverworld().getTime());
        }
    }

    private static void release(ServerPlayerEntity player) {
        if (CarryOnDataManager.getCarryData(player).isCarrying(CarryOnData.CarryType.PLAYER)) {
            PlacementHandler.placeCarried(player);
        }
        if (player.getVehicle() instanceof ServerPlayerEntity carrier && carried(player)) {
            player.stopRiding();
            clearIfStale(carrier);
        }
    }

    private static void clearIfStale(ServerPlayerEntity carrier) {
        CarryOnData carry = CarryOnDataManager.getCarryData(carrier);
        if (carry.isCarrying(CarryOnData.CarryType.PLAYER)
            && carrier.getPassengerList().stream().noneMatch(e -> e instanceof PlayerEntity)) {
            carry.clear();
            CarryOnDataManager.setCarryData(carrier, carry);
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            clearIfStale(player);
        }
        CarryEscapeState s = state;
        if (s != null && server.getTicks() % 1200 == 0) {
            s.prune(server.getOverworld().getTime());
        }
    }
}
