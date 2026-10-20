package dev.councilcraft.mixin;

import dev.councilcraft.coin.NumismaticsText;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** PM32 / PM32b. Coin tooltip value line in diamonds ("Value: 25 Diamonds"). */
@Pseudo
@Mixin(targets = "dev.ithundxr.createnumismatics.content.coins.CoinItem", remap = false)
public abstract class NumismaticsCoinValueTextMixin {
    @Redirect(method = "method_7851", remap = false, at = @At(value = "INVOKE", remap = false,
        target = "Lnet/minecraft/class_2561;method_43469(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;"))
    private MutableText councilcraft$value(String key, Object[] args) {
        return NumismaticsText.translatable(key, args);
    }
}
