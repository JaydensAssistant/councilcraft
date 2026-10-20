package dev.councilcraft;

import dev.councilcraft.handshake.LoginHandshake;
import dev.councilcraft.respawn.RebornClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client half of the login handshake, and the Reborn active-time packet.
 * Dedicated servers do not load this class.
 */
public final class CouncilCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LoginHandshake.registerClient();
        RebornClient.register();
        dev.councilcraft.respawn.RebornFeedingClient.register();
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("distanthorizons") && loader.isModLoaded("imm_ptl_core")) {
            // PM21. Separate class so DH and IP types load only when both mods are present.
            dev.councilcraft.compat.DhPortalDeferredFix.register();
        }
    }
}
