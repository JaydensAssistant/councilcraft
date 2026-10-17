package dev.councilcraft.mixin;

import dev.councilcraft.rules.HorseSpeed;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S244. An unridden AbstractHorse wanders with {@code getMovementSpeed}.
 * Ridden movement uses {@code getSaddledSpeed} instead, which the other
 * mixins cap. Both sides run this, so the client predicts the same cap.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHorseSpeedMixin {
    @Inject(method = "getMovementSpeed()F", at = @At("RETURN"), cancellable = true)
    private void councilcraft$capHorse(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof AbstractHorseEntity) {
            cir.setReturnValue(HorseSpeed.cap(cir.getReturnValueF()));
        }
    }
}
