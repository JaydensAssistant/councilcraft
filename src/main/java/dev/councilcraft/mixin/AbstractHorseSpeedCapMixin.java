package dev.councilcraft.mixin;

import dev.councilcraft.rules.HorseSpeed;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S221 / S244. A ridden horse, donkey, mule, llama or zebra moves at
 * {@code getSaddledSpeed}, which reads the movement attribute (shoes, potions
 * and Soul Speed included) and does not go through {@code getMovementSpeed}.
 * Camel overrides this method, so it has its own mixin.
 */
@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseSpeedCapMixin {
    @Inject(method = "getSaddledSpeed", at = @At("RETURN"), cancellable = true)
    private void councilcraft$capSaddled(PlayerEntity rider, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(HorseSpeed.cap(cir.getReturnValueF()));
    }
}
