package dev.councilcraft.mixin;

import dev.councilcraft.compat.Zebras;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPPM3. A chested zebra has one more inventory slot than a donkey, because its armor (1) and horseshoes (2) both
 * come before the chest. {@code ZebraScreenHandlerMixin} moves the chest slots to start at index 3.
 */
@Mixin(AbstractDonkeyEntity.class)
public abstract class ZebraInventoryMixin {
    @Inject(method = "getInventorySize", at = @At("RETURN"), cancellable = true)
    private void councilcraft$zebraInventory(CallbackInfoReturnable<Integer> cir) {
        AbstractDonkeyEntity donkey = (AbstractDonkeyEntity) (Object) this;
        if (donkey.hasChest() && Zebras.isZebra(donkey)) {
            cir.setReturnValue(cir.getReturnValueI() + 1);
        }
    }
}
