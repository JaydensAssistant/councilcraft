package dev.councilcraft.mixin;

import dev.councilcraft.rules.HorseSpeed;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S244. Camel does not call {@code AbstractHorse.getSaddledSpeed}. While the
 * rider sprints it adds 0.1 on top of the attribute. The cap applies to that
 * total, so a sprinting camel still cannot pass 0.44.
 */
@Mixin(CamelEntity.class)
public abstract class CamelSpeedCapMixin {
    @Inject(method = "getSaddledSpeed", at = @At("RETURN"), cancellable = true)
    private void councilcraft$capCamel(PlayerEntity rider, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(HorseSpeed.cap(cir.getReturnValueF()));
    }
}
