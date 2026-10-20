package dev.councilcraft.mixin;

import dev.councilcraft.coin.NumismaticsText;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** PM32a. Vendor and brass depositor goggle tooltips: price as coins, no "N¤" tail. */
@Pseudo
@Mixin(targets = {
    "dev.ithundxr.createnumismatics.content.vendor.VendorBlockEntity",
    "dev.ithundxr.createnumismatics.content.depositor.BrassDepositorBlockEntity"
}, remap = false)
public abstract class NumismaticsTooltipPriceMixin {
    @Redirect(method = "addToTooltip", remap = false, at = @At(value = "INVOKE", remap = false,
        target = "Lnet/minecraft/class_2561;method_43469(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;"))
    private MutableText councilcraft$price(String key, Object[] args) {
        return NumismaticsText.translatable(key, args);
    }
}
