package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.talhanation.smallships.network.packet.ServerboundShipDetachPacket;
import dev.councilcraft.compat.FlagBanners;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PT43. Small Ships 2.0.1 ServerboundShipDetachPacket.giveBack: the banner taken off a ship goes to the player's
 * inventory (or the ground). A marked banner goes back as the identical flag. Pinned to Small Ships 2.0.1 and
 * Supplementaries 1.20-3.1.44.
 */
@Mixin(value = ServerboundShipDetachPacket.class, remap = false)
public abstract class ShipDetachFlagMixin {
    @WrapOperation(method = "giveBack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/player/PlayerInventory;offerOrDrop(Lnet/minecraft/item/ItemStack;)V", remap = true), require = 1)
    private static void councilcraft$bannerBecomesFlag(PlayerInventory inventory, ItemStack stack, Operation<Void> original) {
        original.call(inventory, FlagBanners.restore(stack));
    }
}
