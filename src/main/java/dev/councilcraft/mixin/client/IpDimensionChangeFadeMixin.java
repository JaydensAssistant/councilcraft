package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.client.PortalFade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT13. Starts the portal fade the moment Immersive Portals 5.2.0 moves the client player into another
 * dimension, in the same frame. PortalFade's per-tick dimension check is the fallback.
 * Applied only with Immersive Portals 5.2.0 and Distant Horizons; pinned in CouncilCraftMixinPlugin.
 */
@Pseudo
@Mixin(targets = "qouteall.imm_ptl.core.teleportation.ClientTeleportationManager", remap = false)
public abstract class IpDimensionChangeFadeMixin {
    @Inject(method = "changePlayerDimension", at = @At("TAIL"), remap = false)
    private static void councilcraft$startPortalFade(CallbackInfo ci) {
        PortalFade.begin();
    }
}
