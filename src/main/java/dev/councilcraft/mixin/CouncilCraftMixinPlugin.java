package dev.councilcraft.mixin;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
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
        {"supplementaries", "sacks barred from bundles and toolboxes (S236)"},
        {"create", "toolbox whitelist (S234) and basin minting"},
        {"numismatics", "coin values (S123)"},
        {"pehkui", "baby respawn size (S80)"},
        {"create_enchantment_industry", "Blaze Forger clamp and printed copies (S177 / S207)"},
        {"exposure", "exposure upload size guard (PM26)"}
    };

    /** Mixins that hook an optional mod: mixin class name, then the mod ids it needs. */
    private static final String[][] OPTIONAL_MIXINS = {
        {"dev.councilcraft.mixin.SackEncumbranceMixin", "supplementaries", "inventorio"},
        {"dev.councilcraft.mixin.client.MutantNameplateMixin", "nameplate"},
        {"dev.councilcraft.mixin.client.GeoNameplateMixin", "nameplate", "geckolib"},
        {"dev.councilcraft.mixin.client.ParagliderWheelMixin", "paraglider"},
        {"dev.councilcraft.mixin.client.StaminaXpBarMixin", "paraglider"},
        {"dev.councilcraft.mixin.client.AntiqueAtlasItemMixin", "antique_atlas"}
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
        // Class names are checked later, from CouncilCraft.onInitialize.
        // Doing it here would load the class before this config's mixins are
        // registered, and a @Pseudo mixin would then miss its target quietly.
        LOGGER.info("CouncilCraft required mods present ({}).", REQUIRED_MODS.length);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return shouldApply(mixinClassName, FabricLoader.getInstance()::isModLoaded);
    }

    /** False only for an optional-mod mixin whose mods are not all loaded. */
    static boolean shouldApply(String mixinClassName, Predicate<String> isModLoaded) {
        for (String[] optional : OPTIONAL_MIXINS) {
            if (optional[0].equals(mixinClassName)) {
                for (int i = 1; i < optional.length; i++) {
                    if (!isModLoaded.test(optional[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
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
