package dev.councilcraft.mixin.client;

import com.goby56.wakes.event.WakeWorldTicker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM80 (A). Immersive Portals ticks remote dimensions on the client and fires END_WORLD_TICK for each, so Wakes
 * 0.4.0 ran its tick (and recoloured wakes with that dimension's light) once per rendered remote world. Wakes only
 * belong to the player's own world, so the tick runs only for it. Applied only with Wakes.
 */
@Mixin(value = WakeWorldTicker.class, remap = false)
public abstract class WakesRemoteTickMixin {
    @Inject(method = "onEndTick", at = @At("HEAD"), cancellable = true, require = 1)
    private void councilcraft$ownWorldOnly(ClientWorld world, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && world != client.player.getWorld()) {
            ci.cancel();
        }
    }
}
