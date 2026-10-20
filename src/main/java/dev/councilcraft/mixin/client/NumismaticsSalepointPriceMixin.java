package dev.councilcraft.mixin.client;

import dev.councilcraft.coin.NumismaticsText;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** PM32a. Salepoint goggle tooltip: price per unit as coins. */
@Pseudo
@Mixin(targets = "dev.ithundxr.createnumismatics.content.salepoint.SalepointBlockEntity", remap = false)
public abstract class NumismaticsSalepointPriceMixin {
    @Redirect(method = "createTooltip", remap = false, at = @At(value = "INVOKE", remap = false,
        target = "Lnet/minecraft/class_2561;method_43469(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;"))
    private MutableText councilcraft$price(String key, Object[] args) {
        return NumismaticsText.translatable(key, args);
    }
}
