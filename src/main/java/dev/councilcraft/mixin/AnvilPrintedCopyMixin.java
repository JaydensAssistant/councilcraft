package dev.councilcraft.mixin;

import dev.councilcraft.enchant.PrintedCopies;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * S242. The anvil result keeps the base item's printed mark.
 * A printed book in the sacrifice slot does not spread.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilPrintedCopyMixin {
    @Inject(method = "updateResult", at = @At("RETURN"))
    private void councilcraft$printed(CallbackInfo ci) {
        ScreenHandler handler = (ScreenHandler) (Object) this;
        PrintedCopies.copyStatus(handler.getSlot(0).getStack(), handler.getSlot(2).getStack());
    }
}
