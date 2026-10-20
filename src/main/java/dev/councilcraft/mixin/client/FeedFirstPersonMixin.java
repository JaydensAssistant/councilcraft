package dev.councilcraft.mixin.client;

import dev.councilcraft.respawn.RebornFeeding;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** PM24b: first person, the feeder's food is held steady (BLOCK pose) instead of the eating bob. */
@Mixin(HeldItemRenderer.class)
public abstract class FeedFirstPersonMixin {
    @Redirect(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;"))
    private UseAction councilcraft$feedPose(ItemStack stack, AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                           Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                           MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        return RebornFeeding.isFeeding(player) ? UseAction.BLOCK : stack.getUseAction();
    }
}
