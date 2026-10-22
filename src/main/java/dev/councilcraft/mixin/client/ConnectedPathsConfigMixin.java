package dev.councilcraft.mixin.client;

import com.connectedpaths.core.config.ConfigLoader;
import com.connectedpaths.core.model.Configs;
import dev.councilcraft.client.compat.ConnectedPathsTweaks;
import java.io.File;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM67. Every Connected Paths config load (the runner's and the compiler's) passes through
 * {@link ConnectedPathsTweaks#apply}: logs, woods and obsidians out of melding, GrassFix off.
 * Applied only with Connected Paths.
 */
@Mixin(value = ConfigLoader.class, remap = false)
public abstract class ConnectedPathsConfigMixin {
    @Inject(method = "loadConfigs", at = @At("RETURN"), require = 1)
    private static void councilcraft$applyRules(File configDir, CallbackInfoReturnable<Configs> cir) {
        ConnectedPathsTweaks.apply(cir.getReturnValue());
    }
}
