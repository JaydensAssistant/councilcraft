package dev.councilcraft.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tschipp.carryon.common.carry.CarryOnDataManager;

/**
 * PT70. Punchy 2.8g's SwimmingStateMachine must not play swim strokes while the local player
 * is carrying with Carry On. Pinned to Punchy 2.8g; require=0 for another build.
 */
@Pseudo
@Mixin(targets = "punchy.client.state.SwimmingStateMachine", remap = false)
public class PunchySwimCarryMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void councilcraft$skipWhileCarrying(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        try {
            if (CarryOnDataManager.getCarryData(player).isCarrying()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // Carry On client path unavailable
        }
    }
}
