package dev.councilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.rubixdev.inventorio.client.ui.InventorioScreen;
import de.rubixdev.inventorio.packet.InventorioNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PPM86. Controlify's inventory button opens the vanilla {@link InventoryScreen} directly
 * ({@code InGameInputHandler.handleKeybinds}, INVENTORY case), skipping Inventorio's keyboard hook.
 * Here it opens Inventorio's screen under Inventorio's own conditions: the vanilla screen stays when
 * Inventorio's vanilla-inventory toggle is on or the player has the creative inventory.
 * Applied only with Controlify and Inventorio.
 */
@Pseudo
@Mixin(targets = "dev.isxander.controlify.ingame.InGameInputHandler", remap = false)
public abstract class ControllerInventorioScreenMixin {
    // The first setScreen in handleKeybinds is the INVENTORY case's setScreen(new InventoryScreen(player)).
    @WrapOperation(method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
            ordinal = 0, remap = true),
        require = 1)
    private void councilcraft$openInventorio(MinecraftClient client, Screen screen, Operation<Void> original) {
        if (!(screen instanceof InventoryScreen) || InventorioScreen.shouldOpenVanillaInventory
            || client.interactionManager != null && client.interactionManager.hasCreativeInventory()) {
            original.call(client, screen);
            return;
        }
        InventorioNetworking.getInstance().c2sOpenInventorioScreen();
    }
}
