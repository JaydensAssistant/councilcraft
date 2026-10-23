package dev.councilcraft.client.compat;

import dev.councilcraft.CouncilCraft;
import punchy.config.PunchyConfig;

/**
 * PPM101, client only, loaded only when Punchy is present. The Vanilla look also turns Punchy's first-person
 * animations off (and back on), through Punchy's own config API so its file and in-memory state stay in step.
 * Punchy is pinned in the pack; if a future Punchy renames these members the toggle still works and this logs.
 */
public final class PunchyVanillaLook {
    private PunchyVanillaLook() {
    }

    /** Sets Punchy's "enable mod" to the opposite of the Vanilla look. Saves only when it changes. */
    public static void apply(boolean vanillaLookOn) {
        try {
            PunchyConfig.Data data = PunchyConfig.snapshot();
            boolean wanted = !vanillaLookOn;
            if (data.enableMod != wanted) {
                data.enableMod = wanted;
                PunchyConfig.applyAndSave(data);
                CouncilCraft.LOGGER.info("CouncilCraft vanilla look: Punchy {}", wanted ? "on" : "off");
            }
        } catch (LinkageError | RuntimeException e) {
            CouncilCraft.LOGGER.warn("CouncilCraft vanilla look could not switch Punchy", e);
        }
    }
}
