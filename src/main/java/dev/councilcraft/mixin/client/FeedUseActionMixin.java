package dev.councilcraft.mixin.client;

import dev.councilcraft.respawn.RebornFeeding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT62. Punchy 2.8g's ConsumeStateMachine reads ItemStack.getUseAction for EAT/DRINK.
 * While the local player is feeding a Reborn baby, return BLOCK so Punchy does not play
 * the eat/drink clip. Eating and drinking normally are unchanged.
 */
@Mixin(ItemStack.class)
public abstract class FeedUseActionMixin {
    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void councilcraft$feedPose(CallbackInfoReturnable<UseAction> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null || !RebornFeeding.isFeeding(player)) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (player.getActiveItem() != self && player.getMainHandStack() != self && player.getOffHandStack() != self) {
            // Only the held feeding stack
            if (!player.isUsingItem() || player.getActiveItem() != self) {
                return;
            }
        }
        if (!player.isUsingItem()) {
            return;
        }
        UseAction own = self.getItem().getUseAction(self);
        if (own == UseAction.EAT || own == UseAction.DRINK) {
            cir.setReturnValue(UseAction.BLOCK);
        }
    }
}
