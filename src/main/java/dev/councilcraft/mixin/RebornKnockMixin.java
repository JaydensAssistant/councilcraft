package dev.councilcraft.mixin;

import dev.councilcraft.respawn.RebornMotion;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Knockback is server velocity the client then coasts. It is not a step. */
@Mixin(LivingEntity.class)
public class RebornKnockMixin {
    @Inject(method = "takeKnockback", at = @At("HEAD"))
    private void councilcraft$knock(double strength, double x, double z, CallbackInfo ci) {
        if (strength <= 0.0 || !((Object) this instanceof ServerPlayerEntity player) || player.getServer() == null) {
            return;
        }
        RebornMotion.pushed(player, player.getServer().getTicks());
    }
}
