package dev.councilcraft.mixin;

import dev.councilcraft.print.PrintInkHints;
import java.util.List;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBehaviour;

/**
 * PT16. With no template set, the Printer's goggle tooltip (its ink tank) names the inks CouncilCraft's photo
 * printing takes. With a photo or album template the existing PT14 goggles already say which ink it needs.
 */
@Pseudo
@Mixin(targets = "plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBlockEntity")
public abstract class PrinterInkGogglesMixin {
    @Shadow(remap = false)
    PrinterBehaviour printer;

    @Inject(method = "addToGoggleTooltip(Ljava/util/List;Z)Z", at = @At("RETURN"), cancellable = true, remap = false)
    private void councilcraft$inkHint(List<Text> tooltip, boolean sneaking, CallbackInfoReturnable<Boolean> cir) {
        if (this.printer != null && this.printer.getFilter().isEmpty()) {
            PrintInkHints.goggles(tooltip);
            cir.setReturnValue(true);
        }
    }
}
