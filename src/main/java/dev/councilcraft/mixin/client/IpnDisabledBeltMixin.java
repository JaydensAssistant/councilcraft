package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.InventorioBeltSlots;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT76. IPN's AreaTypes$disabled$1: treat Inventorio tool belt and utility belt slots as
 * disabled so player-side sort never touches them. Deep Pockets stay enabled.
 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.AreaTypes$disabled$1", remap = false)
public class IpnDisabledBeltMixin {
    @Inject(method = "invoke", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void councilcraft$disableBelts(Slot slot, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (InventorioBeltSlots.isExcludedFromContainerSort(slot)) {
            // Only belts — crafting is handled by ignoreCraftingGrid hint / itemStorage filter.
            // isExcluded also flags crafting; for disabled$1 that is fine (player path).
            cir.setReturnValue(true);
        }
    }
}
