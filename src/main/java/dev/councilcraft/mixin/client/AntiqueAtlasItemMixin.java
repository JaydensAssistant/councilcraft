package dev.councilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.councilcraft.item.AntiqueAtlasItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PM25, Antique Atlas 3.1.2 (client-only mod). (1) Only councilcraft:antique_atlas counts as an atlas,
 * so a book renamed "Antique Atlas" does nothing. (2) The map key needs the atlas in the inventory
 * (requireItem forced on, whatever the client config says). (3) Antique Atlas's own creative-tab
 * atlas becomes our item.
 */
@Pseudo
@Mixin(targets = "folk.sisby.antique_atlas.AntiqueAtlas", remap = false)
public abstract class AntiqueAtlasItemMixin {
    @Inject(method = "isHandheldAtlas", at = @At("HEAD"), cancellable = true)
    private static void councilcraft$onlyOurAtlas(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(stack.isOf(AntiqueAtlasItem.ATLAS));
    }

    @Inject(method = "getHandheldAtlas", at = @At("HEAD"), cancellable = true)
    private static void councilcraft$ourAtlasStack(CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(new ItemStack(AntiqueAtlasItem.ATLAS));
    }

    @ModifyExpressionValue(
        method = "openAtlasScreen",
        at = @At(value = "FIELD", target = "Lfolk/sisby/antique_atlas/AntiqueAtlasConfig;requireItem:Z")
    )
    private static boolean councilcraft$requireItem(boolean configured) {
        return true;
    }
}
