package dev.councilcraft.mixin.client;

import dev.councilcraft.coin.NumismaticsText;
import dev.ithundxr.createnumismatics.content.backend.behaviours.SliderStylePriceBehaviour;
import java.util.List;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** PM32a / PPM8. Vendor, brass and andesite depositor goggle tooltips: price as one money total, no "N¤" tail, no coin breakdown lines. */
@Pseudo
@Mixin(targets = {
    "dev.ithundxr.createnumismatics.content.vendor.VendorBlockEntity",
    "dev.ithundxr.createnumismatics.content.depositor.BrassDepositorBlockEntity",
    "dev.ithundxr.createnumismatics.content.depositor.AndesiteDepositorBlockEntity"
}, remap = false)
public abstract class NumismaticsTooltipPriceMixin {
    @Redirect(method = "addToTooltip", remap = false, at = @At(value = "INVOKE", remap = false,
        target = "Lnet/minecraft/class_2561;method_43469(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;"))
    private MutableText councilcraft$price(String key, Object[] args) {
        return NumismaticsText.translatable(key, args);
    }

    /** The money total replaces the per-coin lines Numismatics adds under the price. */
    @Redirect(method = "addToTooltip", remap = false, at = @At(value = "INVOKE", remap = false,
        target = "Ldev/ithundxr/createnumismatics/content/backend/behaviours/SliderStylePriceBehaviour;getCondensedPriceBreakdown()Ljava/util/List;"))
    private List<MutableText> councilcraft$noBreakdown(SliderStylePriceBehaviour price) {
        return List.of();
    }
}
