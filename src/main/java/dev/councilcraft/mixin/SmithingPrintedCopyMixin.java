package dev.councilcraft.mixin;

import dev.councilcraft.enchant.PrintedCopies;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmithingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * S242. Smithing keeps the base item's printed mark. The template and the
 * addition do not spread it, and they do not clear it.
 */
@Mixin(SmithingScreenHandler.class)
public abstract class SmithingPrintedCopyMixin {
    @Inject(method = "updateResult", at = @At("RETURN"))
    private void councilcraft$printed(CallbackInfo ci) {
        ScreenHandler handler = (ScreenHandler) (Object) this;
        PrintedCopies.copyStatus(handler.getSlot(1).getStack(), handler.getSlot(3).getStack());
    }
}
