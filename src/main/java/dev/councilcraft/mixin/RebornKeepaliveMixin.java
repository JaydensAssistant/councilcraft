package dev.councilcraft.mixin;

import dev.councilcraft.respawn.RebornStatusEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla resends a status effect whenever its duration is a multiple of 600.
 * A frozen duration that is itself a multiple of 600 made that true every tick,
 * which is the packet flood. The duration now counts down, and this skips that
 * keepalive for Reborn only, so the effect packet is the one written at a
 * window boundary (and on respawn), not one every 30 seconds. The stored
 * duration is not changed.
 */
@Mixin(LivingEntity.class)
public abstract class RebornKeepaliveMixin {
    @Redirect(
        method = "tickStatusEffects",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/effect/StatusEffectInstance;getDuration()I"
        )
    )
    private int councilcraft$noRebornKeepalive(StatusEffectInstance effect) {
        int duration = effect.getDuration();
        if (effect.getEffectType() == RebornStatusEffect.EFFECT && duration % 600 == 0) {
            return duration - 1;
        }
        return duration;
    }
}
