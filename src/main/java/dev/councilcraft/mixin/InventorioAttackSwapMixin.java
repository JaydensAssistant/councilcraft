package dev.councilcraft.mixin;

import de.rubixdev.inventorio.player.inventory.PlayerInventoryExtraStuff;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM68. Attacks always use the real main-hand item (Better Combat reads it). Inventorio swaps the
 * best tool-belt weapon into the main hand around {@code PlayerEntity.attack} and back after; both
 * halves are cancelled. The axe slot stays for automatic axe selection when breaking wood. The
 * forced-full attack indicator only triggers for a belt item that fits a sword, so it goes with the
 * sword slot. Applied only with Inventorio.
 */
@Mixin(value = PlayerInventoryExtraStuff.class, remap = false)
public abstract class InventorioAttackSwapMixin {
    @Inject(method = "prePlayerAttack", at = @At("HEAD"), cancellable = true)
    private void councilcraft$noPreAttackSwap(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "postPlayerAttack", at = @At("HEAD"), cancellable = true)
    private void councilcraft$noPostAttackSwap(CallbackInfo ci) {
        ci.cancel();
    }
}
