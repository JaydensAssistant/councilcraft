package dev.councilcraft.mixin;

import net.blay09.mods.hardcorerevival.api.HardcoreRevivalAPI;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT55. Server side: while knocked out, refuse InventorioScreenHandler.Companion.open so the
 * inventory cannot open through Inventorio's C2S open_screen packet.
 */
@Mixin(targets = "de.rubixdev.inventorio.player.InventorioScreenHandler$Companion", remap = false)
public abstract class InventorioKnockoutOpenMixin {
    @Inject(method = "open", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void councilcraft$refuseWhileDowned(PlayerEntity player, CallbackInfo ci) {
        if (player != null && HardcoreRevivalAPI.isKnockedOut(player)) {
            ci.cancel();
        }
    }
}
