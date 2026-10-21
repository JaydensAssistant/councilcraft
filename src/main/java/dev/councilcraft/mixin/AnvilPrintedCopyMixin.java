package dev.councilcraft.mixin;

import dev.councilcraft.enchant.PrintedCopies;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * S242 / PPM51 copy-wins. The anvil result is printed when either input is printed, so a copy can never be
 * laundered into an original by combining it with one. Easy Anvils replaces this method; see
 * {@link EasyAnvilPrintedCopyMixin}.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilPrintedCopyMixin {
    @Inject(method = "updateResult", at = @At("RETURN"))
    private void councilcraft$printed(CallbackInfo ci) {
        ScreenHandler handler = (ScreenHandler) (Object) this;
        PrintedCopies.copyWins(handler.getSlot(2).getStack(), handler.getSlot(0).getStack(), handler.getSlot(1).getStack());
    }
}
