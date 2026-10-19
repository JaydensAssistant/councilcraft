package dev.councilcraft.mixin;

import dev.councilcraft.rules.ParagliderRepair;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tictim.paraglider.fabric.contents.item.FabricParagliderItem;

/**
 * S244. Paragliders 20.1.4 has no repair-item config. {@code FabricParagliderItem.canRepair}
 * (Mojmap {@code isValidRepairItem}) returns true only for leather. Phantom membrane
 * is accepted as well. The durability value itself (384) is the pack's Paragliders config.
 */
@Mixin(FabricParagliderItem.class)
public abstract class ParagliderRepairMixin {
    @Inject(method = "canRepair", at = @At("RETURN"), cancellable = true)
    private void councilcraft$phantomMembrane(ItemStack toRepair, ItemStack repair, CallbackInfoReturnable<Boolean> cir) {
        if (ParagliderRepair.accepts(cir.getReturnValueZ(), repair.isOf(Items.PHANTOM_MEMBRANE))) {
            cir.setReturnValue(true);
        }
    }
}
