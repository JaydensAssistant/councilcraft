package dev.councilcraft.mixin;

import dev.councilcraft.enchant.PrintedCopies;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * S242 / PPM51 copy-wins. The smithing result is printed when any input (template, base or addition) is printed.
 */
@Mixin(SmithingScreenHandler.class)
public abstract class SmithingPrintedCopyMixin {
    @Inject(method = "updateResult", at = @At("RETURN"))
    private void councilcraft$printed(CallbackInfo ci) {
        ScreenHandler handler = (ScreenHandler) (Object) this;
        PrintedCopies.copyWins(handler.getSlot(3).getStack(),
            handler.getSlot(0).getStack(), handler.getSlot(1).getStack(), handler.getSlot(2).getStack());
    }
}
