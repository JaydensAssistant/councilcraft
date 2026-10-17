package dev.councilcraft.mixin;

import dev.councilcraft.rules.SackNesting;
import net.mehvahdjukaar.supplementaries.common.items.SackItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S236. Yarn calls this {@code canBeNested}; Mojmap calls it {@code canFitInsideContainerItems}.
 * Bundles and Create's toolbox both refuse an item that returns false. Supplementaries
 * 1.20-3.1.44 already returns false; this mixin is the lock.
 */
@Mixin(SackItem.class)
public abstract class SackNestingMixin {
    @Inject(method = "canBeNested", at = @At("HEAD"), cancellable = true)
    private void councilcraft$barFromContainers(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(SackNesting.canFitInsideContainerItems());
    }
}
