package dev.councilcraft.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * A missing hooked mod, or a renamed mixin target, must fail in the log.
 * Mixin itself crashes the game when a required target method is gone
 * ({@code defaultRequire: 1} in the mixin json). This plugin does the same
 * for a mod id that is not installed at all, and names the hook in the message
 * so the log says which behaviour broke.
 */
public final class CouncilCraftMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("councilcraft");

    /** Mod id, then the hook that needs it. */
    private static final String[][] REQUIRED_MODS = {
        {"horseshoes", "gold horseshoe loot block (S245)"},
        {"mutantmonsters", "mutant creeper / minion no block damage (S224)"},
        {"paraglider", "paraglider repair with phantom membrane (S244)"},
        {"supplementaries", "sacks barred from bundles and toolboxes (S236)"}
    };

    @Override
    public void onLoad(String mixinPackage) {
        FabricLoader loader;
        try {
            loader = FabricLoader.getInstance();
        } catch (Throwable notARunningGame) {
            // Loom's mixin annotation processor loads this class while compiling.
            // FabricLoader throws if the game has not started. That is not a missing mod.
            return;
        }
        for (String[] required : REQUIRED_MODS) {
            if (!loader.isModLoaded(required[0])) {
                String message = "CouncilCraft hook failed: " + required[1]
                    + ". Mod '" + required[0] + "' is not loaded. Refusing to continue with the hook silently skipped.";
                LOGGER.error(message);
                throw new IllegalStateException(message);
            }
        }
        LOGGER.info("CouncilCraft mixin targets present for {} hooked mod(s).", REQUIRED_MODS.length);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
