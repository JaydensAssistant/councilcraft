package dev.councilcraft.compat;

import dev.councilcraft.rules.CarryWeight;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;

/**
 * PPM35 / PT54 / PT57. Carry On's carry weight shows as Supplementaries' overencumbered effect.
 * PT54: a carried player maps to amplifier 0. PT57: applied state keyed by UUID.
 */
public final class CarryOnWeight {
    public static final Identifier OVERENCUMBERED = new Identifier("supplementaries", "overencumbered");

    private record Applied(int amplifier, long tick) {
    }

    private static final Map<UUID, Applied> APPLIED = new HashMap<>();

    private CarryOnWeight() {
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> disconnect(handler.player.getUuid()));
    }

    private static StatusEffect effect() {
        return Registries.STATUS_EFFECT.get(OVERENCUMBERED);
    }

    /** In place of Carry On's addStatusEffect(Slowness). Falls back to Carry On's effect without Supplementaries. */
    public static boolean apply(ServerPlayerEntity player, StatusEffectInstance carryOnSlowness) {
        StatusEffect effect = effect();
        if (effect == null) {
            return player.addStatusEffect(carryOnSlowness);
        }
        boolean carriedIsPlayer = CarryOnDataManager.getCarryData(player).isCarrying(CarryOnData.CarryType.PLAYER);
        int amplifier = CarryWeight.amplifier(carryOnSlowness.getAmplifier(), carriedIsPlayer);
        long now = player.getServer() == null ? 0L : player.getServer().getTicks();
        UUID id = player.getUuid();
        Applied last = APPLIED.get(id);
        StatusEffectInstance current = player.getStatusEffect(effect);
        boolean hasOurs = current != null && current.getAmplifier() >= amplifier;
        if (last != null && !CarryWeight.shouldApply(amplifier, last.amplifier(), now - last.tick(), hasOurs)) {
            return false;
        }
        if (last != null && last.amplifier() > amplifier && current != null && current.getAmplifier() == last.amplifier()) {
            player.removeStatusEffect(effect);
        }
        APPLIED.put(id, new Applied(amplifier, now));
        return player.addStatusEffect(new StatusEffectInstance(effect, CarryWeight.DURATION_TICKS, amplifier, false, false, true));
    }

    /** Every Carry On tick: once the player is no longer carrying, remove the effect we applied. */
    public static void tick(ServerPlayerEntity player, boolean carrying) {
        if (carrying || APPLIED.remove(player.getUuid()) == null) {
            return;
        }
        StatusEffect effect = effect();
        if (effect != null) {
            player.removeStatusEffect(effect);
        }
    }

    /** PT57. Drop the applied entry on disconnect so the UUID map does not leak. */
    public static void disconnect(UUID id) {
        APPLIED.remove(id);
    }
}
