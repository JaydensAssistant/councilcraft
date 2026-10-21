package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.client.PortalHandMask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM7. Right after Immersive Portals' Iris renderer sets up the stencil for a portal layer, mark the
 * first-person hand in the stencil ({@link PortalHandMask}) so the portal view is not pasted over it.
 * Applied only with Iris and IP.
 */
@Pseudo
@Mixin(targets = "qouteall.imm_ptl.core.compat.iris_compatibility.IrisPortalRenderer", remap = false)
public abstract class IrisPortalHandMixin {
    @Inject(method = "tryRenderViewAreaInDeferredBufferAndIncreaseStencil", remap = false, at = @At(value = "INVOKE",
        target = "Lqouteall/imm_ptl/core/compat/iris_compatibility/IrisPortalRenderer;initStencilForLayer(I)V",
        shift = At.Shift.AFTER, remap = false))
    private void councilcraft$keepHand(CallbackInfoReturnable<Boolean> cir) {
        PortalHandMask.markHand();
    }
}
