package dev.councilcraft.mixin;

import dev.councilcraft.respawn.RebornMotion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A piston or a shulker moves the player. The confirming packet is not a step. */
@Mixin(Entity.class)
public class RebornPushMixin {
    @Inject(method = "move", at = @At("HEAD"))
    private void councilcraft$push(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (type != MovementType.PISTON && type != MovementType.SHULKER && type != MovementType.SHULKER_BOX) {
            return;
        }
        if (!((Object) this instanceof ServerPlayerEntity player) || player.getServer() == null) {
            return;
        }
        RebornMotion.pushed(player, player.getServer().getTicks());
    }
}
