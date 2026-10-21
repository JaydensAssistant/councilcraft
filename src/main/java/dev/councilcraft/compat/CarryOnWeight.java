package dev.councilcraft.compat;

import dev.councilcraft.rules.CarryWeight;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * PPM35. Carry On's carry weight shows as Supplementaries' overencumbered effect (with its icon) instead of
 * Carry On's hidden Slowness. {@code CarryOnWeightMixin} sends Carry On's per-tick Slowness here. The effect
 * runs longer than Carry On's and is refreshed only when needed, so the icon does not flicker, and it is
 * cleared as soon as the player stops carrying.
 */
public final class CarryOnWeight {
    public static final Identifier OVERENCUMBERED = new Identifier("supplementaries", "overencumbered");

    private record Applied(int amplifier, long tick) {
    }

    private static final Map<ServerPlayerEntity, Applied> APPLIED = new WeakHashMap<>();

    private CarryOnWeight() {
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
        int amplifier = CarryWeight.amplifier(carryOnSlowness.getAmplifier());
        long now = player.getServer() == null ? 0L : player.getServer().getTicks();
        Applied last = APPLIED.get(player);
        StatusEffectInstance current = player.getStatusEffect(effect);
        boolean hasOurs = current != null && current.getAmplifier() >= amplifier;
        if (last != null && !CarryWeight.shouldApply(amplifier, last.amplifier(), now - last.tick(), hasOurs)) {
            return false;
        }
        // A lighter load: drop the heavier effect we applied, or addStatusEffect would keep it.
        if (last != null && last.amplifier() > amplifier && current != null && current.getAmplifier() == last.amplifier()) {
            player.removeStatusEffect(effect);
        }
        APPLIED.put(player, new Applied(amplifier, now));
        return player.addStatusEffect(new StatusEffectInstance(effect, CarryWeight.DURATION_TICKS, amplifier, false, false, true));
    }

    /** Every Carry On tick: once the player is no longer carrying, remove the effect we applied. */
    public static void tick(ServerPlayerEntity player, boolean carrying) {
        if (carrying || APPLIED.remove(player) == null) {
            return;
        }
        StatusEffect effect = effect();
        if (effect != null) {
            player.removeStatusEffect(effect);
        }
    }
}
