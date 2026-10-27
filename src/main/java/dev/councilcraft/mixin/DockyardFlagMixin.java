package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.talhanation.smallships.world.block.DockyardBlockEntity;
import dev.councilcraft.compat.FlagBanners;
import net.minecraft.util.ItemScatterer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PT43. Small Ships 2.0.1 Dockyard. Gate ({@code startModifyTask}): the banner-to-install check accepts a
 * Supplementaries flag by looking at its banner form; the flag itself is what is consumed from the player and
 * installed. Return ({@code finishModify}): every refund the Dockyard spawns passes through here, so a marked
 * banner (the old banner of a banner or sail banner swap or removal) comes out as the identical flag. Pinned to
 * Small Ships 2.0.1 and Supplementaries 1.20-3.1.44.
 */
@Mixin(value = DockyardBlockEntity.class, remap = false)
public abstract class DockyardFlagMixin {
    @ModifyReceiver(method = "startModifyTask", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;", ordinal = 0, remap = true), require = 1)
    private ItemStack councilcraft$flagPassesBannerGate(ItemStack stack) {
        return FlagBanners.toBanner(stack);
    }

    @WrapOperation(method = "finishModify", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/ItemScatterer;spawn(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V",
        remap = true), require = 1)
    private void councilcraft$refundedBannerBecomesFlag(World world, double x, double y, double z, ItemStack stack,
                                                       Operation<Void> original) {
        original.call(world, x, y, z, FlagBanners.restore(stack));
    }
}
