package dev.councilcraft.mixin;

import dev.councilcraft.respawn.RebornActivity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.TradeOutputSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Taking the villager's output is a completed trade. */
@Mixin(TradeOutputSlot.class)
public class RebornTradeMixin {
    @Inject(method = "onTakeItem", at = @At("TAIL"))
    private void councilcraft$trade(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        RebornActivity.trade(player);
    }
}
