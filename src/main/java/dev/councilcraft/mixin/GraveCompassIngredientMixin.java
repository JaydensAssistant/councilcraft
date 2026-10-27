package dev.councilcraft.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT67 amendment 1. Forgotten Graves Death Compasses (NBT GraveMarker) cannot match any
 * recipe ingredient, so they cannot be used as crafting compasses.
 */
@Mixin(Ingredient.class)
public abstract class GraveCompassIngredientMixin {
    @Inject(method = "test(Lnet/minecraft/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void councilcraft$refuseGraveCompass(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue()) || stack == null || !stack.hasNbt()) {
            return;
        }
        if (stack.getNbt().contains("GraveMarker")) {
            cir.setReturnValue(false);
        }
    }
}
