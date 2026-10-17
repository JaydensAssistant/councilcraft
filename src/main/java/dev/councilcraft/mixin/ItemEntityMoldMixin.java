package dev.councilcraft.mixin;

import dev.councilcraft.item.GovernmentMoldItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S242. A dropped mold does not despawn, does not burn (the item is fireproof),
 * and does not die to cactus, explosions or the void. Death of the player still
 * drops it: nothing in this class keeps the item in the inventory.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMoldMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void councilcraft$keepMold(CallbackInfo ci) {
        ItemEntity entity = (ItemEntity) (Object) this;
        if (!isMold(entity.getStack())) {
            return;
        }
        entity.setNeverDespawn();
        int floor = entity.getWorld().getBottomY();
        if (entity.getY() < floor) {
            entity.setPosition(entity.getX(), floor + 4.0, entity.getZ());
            entity.setVelocity(0.0, 0.0, 0.0);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void councilcraft$noDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (isMold(((ItemEntity) (Object) this).getStack())) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isMold(ItemStack stack) {
        return stack.getItem() instanceof GovernmentMoldItem;
    }
}
