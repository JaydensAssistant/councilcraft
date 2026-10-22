package dev.councilcraft.mixin.client;

import com.goby56.wakes.utils.WakesUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM80 (C). Entities in an Immersive Portals remote world (seen through a portal) no longer place wake trails or
 * fall splashes into the player's world's wake handler. Applied only with Wakes.
 */
@Mixin(value = WakesUtils.class, remap = false)
public abstract class WakesOtherWorldMixin {
    @Inject(method = {"placeWakeTrail", "placeFallSplash"}, at = @At("HEAD"), cancellable = true, require = 2)
    private static void councilcraft$playerWorldOnly(Entity entity, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && entity.getWorld() != client.player.getWorld()) {
            ci.cancel();
        }
    }
}
