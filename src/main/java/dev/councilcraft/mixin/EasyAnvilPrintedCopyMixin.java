package dev.councilcraft.mixin;

import dev.councilcraft.enchant.PrintedCopies;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM51. Easy Anvils 8.0.2's anvil menu overrides {@code updateResult} (intermediary {@code method_24928}) and
 * usually builds the result itself without calling the vanilla method, so {@link AnvilPrintedCopyMixin} does not
 * run there. Its rename path calls the same method. Applied only when Easy Anvils is loaded.
 */
@Pseudo
@Mixin(targets = "fuzs.easyanvils.world.inventory.ModAnvilMenu")
public abstract class EasyAnvilPrintedCopyMixin {
    @Inject(method = "method_24928", at = @At("RETURN"), remap = false)
    private void councilcraft$printed(CallbackInfo ci) {
        ScreenHandler handler = (ScreenHandler) (Object) this;
        PrintedCopies.copyWins(handler.getSlot(2).getStack(), handler.getSlot(0).getStack(), handler.getSlot(1).getStack());
    }
}
