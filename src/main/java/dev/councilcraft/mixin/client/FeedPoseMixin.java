package dev.councilcraft.mixin.client;

import dev.councilcraft.respawn.RebornFeeding;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** PM24b: a player feeding a Reborn player holds the food forward (third person), like a shield. */
@Mixin(PlayerEntityRenderer.class)
public abstract class FeedPoseMixin {
    @Redirect(method = "getArmPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;"))
    private static UseAction councilcraft$feedPose(ItemStack stack, AbstractClientPlayerEntity player, Hand hand) {
        return RebornFeeding.isFeeding(player) ? UseAction.BLOCK : stack.getUseAction();
    }
}
