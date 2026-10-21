package dev.councilcraft.mixin;

import dev.councilcraft.rules.HorseFall;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.MuleEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM19 / PPM19a / PPM19b. Horses, donkeys and mules take no fall damage up to their jump height plus
 * 2 blocks ({@link HorseFall}). Vanilla deals the same amount to each rider. Camels, llamas, skeleton
 * and zombie horses stay vanilla.
 */
@Mixin(AbstractHorseEntity.class)
public abstract class HorseFallMixin {
    @Inject(method = "computeFallDamage", at = @At("HEAD"), cancellable = true)
    private void councilcraft$jumpAwareFall(float fallDistance, float damageMultiplier, CallbackInfoReturnable<Integer> cir) {
        AbstractHorseEntity horse = (AbstractHorseEntity) (Object) this;
        if (horse instanceof HorseEntity || horse instanceof DonkeyEntity || horse instanceof MuleEntity) {
            cir.setReturnValue(HorseFall.damage(fallDistance, damageMultiplier, horse.getJumpStrength()));
        }
    }
}
