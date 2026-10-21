package dev.councilcraft.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

/**
 * PPM33. The server runs on Hard with the difficulty locked. The pack ships no server.properties,
 * so the mod sets it every time the server starts.
 */
public final class DifficultyLock {
    public static final Difficulty DIFFICULTY = Difficulty.HARD;

    private DifficultyLock() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(DifficultyLock::apply);
    }

    private static void apply(MinecraftServer server) {
        server.setDifficulty(DIFFICULTY, true);
        server.setDifficultyLocked(true);
    }
}
