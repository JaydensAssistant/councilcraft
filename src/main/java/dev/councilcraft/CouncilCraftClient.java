package dev.councilcraft;

import dev.councilcraft.compat.client.HorseshoeTooltip;
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
        HorseshoeTooltip.register();
        // PPPM3. Zebras render horse armor.
        dev.councilcraft.compat.client.ZebraArmorFeatureRenderer.register();
        // PPM61. Graphics preset picker (first title screen) and the Video Settings button.
        dev.councilcraft.client.graphics.GraphicsPresets.register();
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("inventorio")) {
            // PPM67. Hold Left Alt (rebindable) and scroll to cycle the utility belt.
            dev.councilcraft.client.input.UtilityBeltScroll.register();
        }
        if (loader.isModLoaded("distanthorizons") && loader.isModLoaded("imm_ptl_core")) {
            // PM21. Separate class so DH and IP types load only when both mods are present.
            dev.councilcraft.compat.DhPortalDeferredFix.register();
        }
    }
}
