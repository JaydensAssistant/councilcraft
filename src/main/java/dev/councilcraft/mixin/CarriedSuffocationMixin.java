package dev.councilcraft.mixin;

import dev.councilcraft.carry.BabyCarry;
import dev.councilcraft.respawn.Reborn;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT63. A carried Reborn baby takes no IN_WALL (suffocation) damage. The vanilla in-wall
 * overlay stays (no x-ray). Other damage types are unchanged.
 */
@Mixin(LivingEntity.class)
public abstract class CarriedSuffocationMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void councilcraft$noInWallWhileCarried(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!source.isOf(DamageTypes.IN_WALL)) {
            return;
        }
        if (Reborn.running(player) && BabyCarry.carried(player)) {
            cir.setReturnValue(false);
        }
    }
}
