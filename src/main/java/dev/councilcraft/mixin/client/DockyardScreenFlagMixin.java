package dev.councilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.talhanation.smallships.client.gui.screens.inventory.DockyardScreen;
import dev.councilcraft.compat.FlagBanners;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PT43. Small Ships 2.0.1 DockyardScreen lists only vanilla BannerItems as banner and sail banner choices
 * ({@code addBannerOptions}) and in the signature that decides when the list is rebuilt
 * ({@code buildOptionSignature}). A Supplementaries flag passes both checks by its banner form. Pinned to Small
 * Ships 2.0.1 and Supplementaries 1.20-3.1.44.
 */
@Mixin(value = DockyardScreen.class, remap = false)
public abstract class DockyardScreenFlagMixin {
    @ModifyReceiver(method = "addBannerOptions", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;", ordinal = 0, remap = true), require = 1)
    private ItemStack councilcraft$flagIsAChoice(ItemStack stack) {
        return FlagBanners.toBanner(stack);
    }

    @ModifyReceiver(method = "buildOptionSignature", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;", ordinal = 0, remap = true), require = 1)
    private ItemStack councilcraft$flagIsInTheSignature(ItemStack stack) {
        return FlagBanners.toBanner(stack);
    }
}
