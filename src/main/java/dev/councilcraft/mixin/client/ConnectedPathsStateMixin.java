package dev.councilcraft.mixin.client;

import com.connectedpaths.core.model.Configs;
import com.connectedpaths.core.state.GenerationStateManager;
import dev.councilcraft.client.compat.ConnectedPathsTweaks;
import java.io.File;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM67. Packs generated before CouncilCraft's rules (or under an older rules version) are rebuilt
 * once: generation is forced while config/connected-paths-compiler/councilcraft-rules.txt is missing
 * or stale, and the marker is written when Connected Paths saves its state after a build.
 * Applied only with Connected Paths.
 */
@Mixin(value = GenerationStateManager.class, remap = false)
public abstract class ConnectedPathsStateMixin {
    @Inject(method = "shouldRegenerate", at = @At("RETURN"), cancellable = true, require = 1)
    private static void councilcraft$regenerateForRules(File baseDir, File outputDir, String modVersion, String textureVersion,
                                                        List<String> loadedMods, Configs configs, String mcVersion,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && !ConnectedPathsTweaks.markerCurrent(baseDir)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "saveState", at = @At("TAIL"), require = 1)
    private static void councilcraft$markRules(File baseDir, String modVersion, String textureVersion, List<String> activeSupportedMods, CallbackInfo ci) {
        ConnectedPathsTweaks.writeMarker(baseDir);
    }
}
