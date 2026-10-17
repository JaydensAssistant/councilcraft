package dev.councilcraft;

import dev.councilcraft.handshake.LoginHandshake;
import dev.councilcraft.respawn.RebornClient;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client half of the login handshake, and the Reborn clock packet.
 * Dedicated servers do not load this class.
 */
public final class CouncilCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LoginHandshake.registerClient();
        RebornClient.register();
    }
}
