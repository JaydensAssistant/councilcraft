package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.councilcraft.carry.BabyCarry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PT22 amendment. Vanilla PlayerEntity.tickRiding dismounts a sneaking rider on the server. When that rider is
 * a player carried with Carry On and the dismount really happened, it is an escape: count it once
 * (CarryEscapeRules). A Carry On place-down calls removeAllPassengers instead and is not counted. Applied only
 * when Carry On is loaded.
 */
@Mixin(PlayerEntity.class)
public abstract class CarryEscapeMixin {
    @WrapOperation(method = "tickRiding",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;stopRiding()V"))
    private void councilcraft$countEscape(PlayerEntity self, Operation<Void> original) {
        boolean carried = self instanceof ServerPlayerEntity && BabyCarry.carried(self);
        original.call(self);
        if (carried && !self.hasVehicle()) {
            BabyCarry.escaped((ServerPlayerEntity) self);
        }
    }
}
