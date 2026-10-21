package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.client.PortalDhDepth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM5. Iris 1.7.6 gives shaders Distant Horizons' depth through {@code DHCompat.getDepthTex} and
 * {@code getDepthTexNoTranslucent}. Inside an Immersive Portals portal view both return an all-far depth
 * texture, so the outer world's LODs no longer show as ghost terrain. Applied only with Iris, DH and IP.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.compat.dh.DHCompat", remap = false)
public abstract class IrisDhPortalDepthMixin {
    @Inject(method = "getDepthTex()I", at = @At("HEAD"), cancellable = true, remap = false)
    private void councilcraft$portalDepth(CallbackInfoReturnable<Integer> cir) {
        if (PortalDhDepth.inPortalView()) {
            cir.setReturnValue(PortalDhDepth.emptyDepthTexture());
        }
    }

    @Inject(method = "getDepthTexNoTranslucent()I", at = @At("HEAD"), cancellable = true, remap = false)
    private void councilcraft$portalDepthNoTranslucent(CallbackInfoReturnable<Integer> cir) {
        if (PortalDhDepth.inPortalView()) {
            cir.setReturnValue(PortalDhDepth.emptyDepthTexture());
        }
    }
}
