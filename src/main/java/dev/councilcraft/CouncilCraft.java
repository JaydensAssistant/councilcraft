package dev.councilcraft;

import dev.councilcraft.handshake.LoginHandshake;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CouncilCraft implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(CouncilCraftRules.MOD_ID);

    @Override
    public void onInitialize() {
        LoginHandshake.registerServer();
        // Mutant Monsters' explosion class keeps its own name (it is not a Minecraft
        // class, so Yarn vs intermediary does not matter). Touching it here means a
        // renamed package fails at startup, in the log, instead of the first time a
        // mutant creeper explodes. The mixin json still requires the method itself.
        try {
            Class.forName("fuzs.mutantmonsters.world.level.MutatedExplosion");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "CouncilCraft hook failed: mutant creeper / minion no block damage (S224). "
                    + "fuzs.mutantmonsters.world.level.MutatedExplosion is missing.",
                e
            );
        }
        LOGGER.info("CouncilCraft {} loaded (client and server).", version());
    }

    /** Version from fabric.mod.json, so the handshake and the jar stay in step. */
    public static String version() {
        return FabricLoader.getInstance()
            .getModContainer(CouncilCraftRules.MOD_ID)
            .orElseThrow()
            .getMetadata()
            .getVersion()
            .getFriendlyString();
    }
}
