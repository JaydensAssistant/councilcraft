package dev.councilcraft.compat.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** PT31. Which client levels Distant Horizons has loaded (fed by DH's level load and unload events). */
public final class PortalFadeLevels {
    private static final Set<String> LOADED = ConcurrentHashMap.newKeySet();

    private PortalFadeLevels() {
    }

    public static void loaded(String dimension) {
        LOADED.add(dimension);
    }

    public static void unloaded(String dimension) {
        LOADED.remove(dimension);
    }

    public static boolean isLoaded(String dimension) {
        return LOADED.contains(dimension);
    }
}
