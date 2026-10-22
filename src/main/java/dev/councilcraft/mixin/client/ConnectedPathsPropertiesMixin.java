package dev.councilcraft.mixin.client;

import com.connectedpaths.core.pipeline.Processor;
import dev.councilcraft.client.compat.ConnectedPathsTweaks;
import dev.councilcraft.rules.ConnectedPathsRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM67. Each generated overlay .properties: stripped blocks (including a template's own
 * matchBlocks additions) leave matchBlocks, and snow-family overlays get faces=top bottom
 * ({@link ConnectedPathsRules#rewrite}). Applied only with Connected Paths.
 */
@Mixin(value = Processor.class, remap = false)
public abstract class ConnectedPathsPropertiesMixin {
    @Inject(method = "generatePropertiesText", at = @At("RETURN"), cancellable = true, require = 1)
    private void councilcraft$rewrite(Processor.ProcessedAsset asset, boolean isTranslucent, String mcVersion, CallbackInfoReturnable<String> cir) {
        String own = asset.originalFile != null && asset.originalFile.metadata != null ? asset.originalFile.metadata.getBlockID() : null;
        cir.setReturnValue(ConnectedPathsRules.rewrite(cir.getReturnValue(), ConnectedPathsTweaks.strippedBlocks(), own, asset.tileName));
    }
}
