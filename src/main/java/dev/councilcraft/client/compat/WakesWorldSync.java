package dev.councilcraft.client.compat;

import com.goby56.wakes.simulation.WakeHandler;
import dev.councilcraft.mixin.client.WakeHandlerAccess;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * PPM80 (B). Wakes keeps one WakeHandler for "the" world and never replaces it when Immersive Portals switches the
 * client world, so wakes from the old dimension stayed (and were tinted with the new one's light). At the start of
 * each client tick, a handler for another world is cleared and replaced by one for the current world. Registered
 * only with Wakes.
 */
public final class WakesWorldSync {
    private WakesWorldSync() {}

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(WakesWorldSync::sync);
    }

    private static void sync(MinecraftClient client) {
        if (client.world == null) {
            return;
        }
        WakeHandler.getInstance().ifPresent(handler -> {
            if (handler.world != client.world) {
                ((WakeHandlerAccess) (Object) handler).councilcraft$reset();
                WakeHandler.init(client.world);
            }
        });
    }
}
