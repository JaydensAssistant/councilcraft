package dev.councilcraft.mixin.client;

import dev.councilcraft.client.compat.ControlifyHints;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT66. While Controlify reports a controller is in use, Better Third Person's camera is off
 * (isCameraOnPlayer → false) so the left stick turns the character with vanilla third person.
 * Pinned to Better Third Person 1.9.0; require=0 skips another build with a warning from the plugin.
 */
@Pseudo
@Mixin(targets = "io.socol.betterthirdperson.impl.ClientAdapter", remap = false)
public class BetterThirdPersonControllerMixin {
    @Inject(method = "isCameraOnPlayer", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void councilcraft$offOnController(CallbackInfoReturnable<Boolean> cir) {
        if (!FabricLoader.getInstance().isModLoaded("controlify")) {
            return;
        }
        try {
            if (ControlifyHints.onController()) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
            // Controlify not ready
        }
    }
}
