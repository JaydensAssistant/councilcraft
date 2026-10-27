package dev.councilcraft.mixin.client;

import net.blay09.mods.hardcorerevival.HardcoreRevival;
import net.blay09.mods.hardcorerevival.api.HardcoreRevivalAPI;
import net.blay09.mods.hardcorerevival.config.HardcoreRevivalConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * PT55. While downed, opening Inventorio's screen (or InventoryScreen as a belt-and-suspenders)
 * becomes Hardcore Revival's KnockoutScreen when allowAcceptingFate is on, otherwise the open
 * is cancelled. HCR's own OpenScreenEvent only watches InventoryScreen; Inventorio sends a
 * C2S open instead of setScreen(InventoryScreen). KnockoutScreen's constructor is
 * package-protected, so we construct it by reflection.
 */
@Mixin(MinecraftClient.class)
public abstract class KnockoutInventoryGuardMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen councilcraft$knockoutInsteadOfInventory(Screen screen) {
        if (screen == null) {
            return null;
        }
        boolean inventorio = "de.rubixdev.inventorio.client.ui.InventorioScreen".equals(screen.getClass().getName());
        boolean inventory = screen instanceof InventoryScreen;
        if (!inventorio && !inventory) {
            return screen;
        }
        MinecraftClient client = (MinecraftClient) (Object) this;
        ClientPlayerEntity player = client.player;
        if (player == null || !player.isAlive()) {
            return screen;
        }
        boolean downed;
        try {
            downed = HardcoreRevival.getClientRevivalData().isKnockedOut()
                || HardcoreRevivalAPI.isKnockedOut(player);
        } catch (Throwable t) {
            return screen;
        }
        if (!downed) {
            return screen;
        }
        try {
            if (HardcoreRevivalConfig.getActive().allowAcceptingFate) {
                return knockoutScreen();
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
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
