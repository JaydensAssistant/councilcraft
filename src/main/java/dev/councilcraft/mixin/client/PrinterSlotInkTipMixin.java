package dev.councilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsClient;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import dev.councilcraft.print.PrintInkHints;
import java.util.List;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBehaviour;

/**
 * PT16. Hovering the Printer's template slot adds a line naming the photo inks under Create's
 * "Filter / Click with item to set" hint. Client only; skipped (stock hint) if Create changes the call.
 */
@Mixin(value = FilteringRenderer.class, remap = false)
public abstract class PrinterSlotInkTipMixin {
    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/ValueSettingsClient;showHoverTip(Ljava/util/List;)V"
        )
    )
    private static void councilcraft$printerInkTip(ValueSettingsClient handler, List<MutableText> tip, Operation<Void> original,
                                                   @Local FilteringBehaviour behaviour) {
        if ((Object) behaviour instanceof PrinterBehaviour) {
            tip.add(PrintInkHints.slotTip());
        }
        original.call(handler, tip);
    }
}
