package dev.councilcraft;

import dev.councilcraft.handshake.LoginHandshake;
import net.fabricmc.api.ClientModInitializer;

/** Client half of the login handshake. Dedicated servers do not load this class. */
public final class CouncilCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LoginHandshake.registerClient();
    }
}
