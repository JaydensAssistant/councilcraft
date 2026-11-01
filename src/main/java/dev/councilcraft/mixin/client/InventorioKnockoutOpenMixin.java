package dev.councilcraft.mixin.client;

import net.blay09.mods.hardcorerevival.HardcoreRevival;
import net.blay09.mods.hardcorerevival.api.HardcoreRevivalAPI;
import net.blay09.mods.hardcorerevival.config.HardcoreRevivalConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CC-97. Inventorio opens via C2S {@code c2sOpenInventorioScreen}, not
 * {@code setScreen(InventoryScreen)}. While knocked out, cancel that open and
 * show Hardcore Revival's KnockoutScreen (when fate-accept is on) so the
 * knockout UI matches the vanilla E key path.
 */
@Pseudo
@Mixin(targets = "de.rubixdev.inventorio.packet.InventorioNetworkingFabric", remap = false)
public class InventorioKnockoutOpenMixin {
    @Inject(method = "c2sOpenInventorioScreen", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void councilcraft$knockoutBlocksInventorio(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        boolean downed;
        try {
            downed = HardcoreRevival.getClientRevivalData().isKnockedOut()
                || HardcoreRevivalAPI.isKnockedOut(player);
        } catch (Throwable t) {
            return;
        }
        if (!downed) {
            return;
        }
        ci.cancel();
        try {
            if (HardcoreRevivalConfig.getActive().allowAcceptingFate) {
                client.setScreen(knockoutScreen());
            }
        } catch (Throwable ignored) {
            // Cancelled open is enough when KnockoutScreen cannot be built.
        }
    }

    private static Screen knockoutScreen() {
        try {
            Class<?> cls = Class.forName("net.blay09.mods.hardcorerevival.client.KnockoutScreen");
            var ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (Screen) ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
