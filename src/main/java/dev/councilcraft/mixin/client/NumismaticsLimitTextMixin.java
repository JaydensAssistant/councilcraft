package dev.councilcraft.mixin.client;

import dev.councilcraft.coin.NumismaticsText;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * PM32a. Card spending limits ("Spent X of Y") as coins. Limit has two describe overloads; the
 * no-arg one only delegates, so the text is built in describe(boolean). Numismatics 1.1.0 calls
 * this from SubAccountListScreen (client only).
 */
@Pseudo
@Mixin(targets = "dev.ithundxr.createnumismatics.content.backend.sub_authorization.Limit", remap = false)
public abstract class NumismaticsLimitTextMixin {
    @Redirect(method = "describe(Z)Lnet/minecraft/class_5250;", remap = false, at = @At(value = "INVOKE", remap = false,
        target = "Lnet/minecraft/class_2561;method_43469(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/class_5250;"))
    private MutableText councilcraft$limit(String key, Object[] args) {
        return NumismaticsText.translatable(key, args);
    }
}
