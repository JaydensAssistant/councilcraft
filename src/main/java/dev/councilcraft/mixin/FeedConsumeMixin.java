package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.councilcraft.respawn.RebornFeeding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PM24a / PM24b. While a player feeds a Reborn player, the finished food goes to the Reborn player
 * (RebornFeeding.finish) and the feeder's own eating sound and crumbs are skipped; RebornFeeding plays
 * them at the Reborn player instead.
 */
@Mixin(LivingEntity.class)
public abstract class FeedConsumeMixin {
    @WrapOperation(
        method = "consumeItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;finishUsing(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack councilcraft$feed(ItemStack stack, World world, LivingEntity user, Operation<ItemStack> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity feeder && RebornFeeding.isFeeding(feeder)) {
            return RebornFeeding.finish(feeder, stack, (s, target) -> original.call(s, world, target));
        }
        return original.call(stack, world, user);
    }

    @Inject(method = "spawnConsumptionEffects", at = @At("HEAD"), cancellable = true)
    private void councilcraft$noFeederCrumbs(ItemStack stack, int particleCount, CallbackInfo ci) {
        if (RebornFeeding.isFeeding((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
