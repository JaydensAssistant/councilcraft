package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.talhanation.smallships.world.entity.ship.abilities.Bannerable;
import dev.councilcraft.compat.FlagBanners;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PT43. Small Ships 2.0.1 {@code Bannerable.interactBanner} (right-click a ship with a banner in hand).
 * Gate: only a vanilla BannerItem passes, so the held stack is shown to the check as its banner form when it is a
 * Supplementaries flag; the real flag is what gets copied onto the ship (and converted by
 * {@link ShipFlagBannerMixin}). Return: the banner already on the ship is dropped when it is replaced; a marked
 * banner is turned back into the identical flag first. Pinned to Small Ships 2.0.1 and Supplementaries 1.20-3.1.44.
 */
@Mixin(value = Bannerable.class, remap = false)
public interface BannerableFlagMixin {
    @ModifyReceiver(method = "interactBanner", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;", ordinal = 0, remap = true), require = 1)
    private static ItemStack councilcraft$flagPassesBannerGate(ItemStack held) {
        return FlagBanners.toBanner(held);
    }

    @ModifyExpressionValue(method = "interactBanner", at = @At(value = "INVOKE",
        target = "Lcom/talhanation/smallships/world/entity/ship/Ship;getData",
        ordinal = 0, remap = false), require = 1)
    private static Object councilcraft$replacedBannerBecomesFlag(Object oldBanner) {
        return FlagBanners.forHandBack(oldBanner);
    }
}
