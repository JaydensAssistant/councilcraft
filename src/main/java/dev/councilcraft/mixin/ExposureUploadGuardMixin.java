package dev.councilcraft.mixin;

import dev.councilcraft.CouncilCraft;
import dev.councilcraft.rules.ExposureUploadGuard;
import io.github.mortuusars.exposure.world.level.storage.ExposureData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PM26 (3): reject uploaded exposure data that is not a normal Exposure frame (see ExposureUploadGuard).
 * Exposure 1.9.22 was built with a newer Loom than ours can remap, so its types come from src/stubs.
 */
@Pseudo
@Mixin(targets = "io.github.mortuusars.exposure.world.level.storage.ExposureRepository", remap = false)
public abstract class ExposureUploadGuardMixin {
    @Inject(method = "receiveClientUpload", at = @At("HEAD"), cancellable = true)
    private void councilcraft$checkUpload(ServerPlayerEntity player, String id, ExposureData exposure, CallbackInfo ci) {
        if (!ExposureUploadGuard.acceptable(exposure.getWidth(), exposure.getHeight())) {
            councilcraft$reject(player, id, exposure.getWidth(), exposure.getHeight());
            ci.cancel();
        }
    }

    @Inject(method = "receiveChunkedClientUploadHeader", at = @At("HEAD"), cancellable = true)
    private void councilcraft$checkHeader(ServerPlayerEntity player, String id, int width, int height,
                                          Identifier palette, ExposureData.Tag tag, CallbackInfo ci) {
        if (!ExposureUploadGuard.acceptable(width, height)) {
            councilcraft$reject(player, id, width, height);
            ci.cancel();
        }
    }

    private static void councilcraft$reject(ServerPlayerEntity player, String id, int width, int height) {
        CouncilCraft.LOGGER.warn("Rejected exposure upload '{}' from {}: {}x{} is not a normal Exposure frame (PM26).",
            id, player.getEntityName(), width, height);
    }
}
