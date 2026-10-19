package dev.councilcraft.mixin;

import dev.councilcraft.respawn.RebornStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla counts a status effect down every tick, on the server and on the
 * client. Reborn's displayed time is remaining active play, so that countdown
 * would move while the player is idle and then jump back at the next window.
 * Holding this one effect's duration still lets the server write a new value
 * at each window boundary. The icon steps down by five minutes when the
 * window counted, and it does not move otherwise.
 */
@Mixin(StatusEffectInstance.class)
public abstract class RebornDurationMixin {
    @Shadow
    public abstract StatusEffect getEffectType();

    @Shadow
    public abstract int getDuration();

    @Inject(method = "updateDuration", at = @At("HEAD"), cancellable = true)
    private void councilcraft$holdReborn(CallbackInfoReturnable<Integer> ci) {
        if (this.getEffectType() == RebornStatusEffect.EFFECT) {
            ci.setReturnValue(this.getDuration());
        }
    }
}
