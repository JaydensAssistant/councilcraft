package dev.councilcraft.mixin;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
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
        {"horseshoes", "horseshoe tiers, ridden step height and shift-click equip (S230 / PPM3 / PPM4)"},
        {"mutantmonsters", "mutant creeper / minion no block damage (S224)"},
        {"paraglider", "paraglider repair with phantom membrane (S244) and offhand paraglider (PPM70)"},
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
        {"dev.councilcraft.mixin.CarryOnWeightMixin", "carryon", "supplementaries"},
        {"dev.councilcraft.mixin.NameplateExcludedNbtMixin", "nameplate"},
        {"dev.councilcraft.mixin.EasyAnvilPrintedCopyMixin", "easyanvils"},
        {"dev.councilcraft.mixin.client.MutantNameplateMixin", "nameplate"},
        {"dev.councilcraft.mixin.client.GeoNameplateMixin", "nameplate", "geckolib"},
        {"dev.councilcraft.mixin.client.ParagliderWheelMixin", "paraglider"},
        {"dev.councilcraft.mixin.client.StaminaXpBarMixin", "paraglider"},
        {"dev.councilcraft.mixin.client.AntiqueAtlasItemMixin", "antique_atlas"},
        {"dev.councilcraft.mixin.client.IrisDhPortalDepthMixin", "iris", "distanthorizons", "imm_ptl_core"},
        {"dev.councilcraft.mixin.client.IrisPortalHandMixin", "iris", "imm_ptl_core"},
        {"dev.councilcraft.mixin.client.IrisPortalFadeUniformMixin", "iris", "distanthorizons", "imm_ptl_core"},
        {"dev.councilcraft.mixin.client.IpDimensionChangeFadeMixin", "distanthorizons", "imm_ptl_core"},
        {"dev.councilcraft.mixin.InventorioSwordSlotMixin", "inventorio"},
        {"dev.councilcraft.mixin.InventorioAttackSwapMixin", "inventorio"},
        {"dev.councilcraft.mixin.InventorioToolBeltSaveMixin", "inventorio"},
        {"dev.councilcraft.mixin.client.UtilityScrollMixin", "inventorio"},
        {"dev.councilcraft.mixin.client.ConnectedPathsConfigMixin", "connectedpaths"},
        {"dev.councilcraft.mixin.client.ConnectedPathsPropertiesMixin", "connectedpaths"},
        {"dev.councilcraft.mixin.client.ConnectedPathsStateMixin", "connectedpaths"},
        {"dev.councilcraft.mixin.client.ControllerRadialDefaultsMixin", "controlify"},
        {"dev.councilcraft.mixin.client.DockyardWoodPreselectMixin", "smallships"},
        {"dev.councilcraft.mixin.client.WakesRemoteTickMixin", "wakes"},
        {"dev.councilcraft.mixin.client.WakeHandlerAccess", "wakes"},
        {"dev.councilcraft.mixin.client.WakesOtherWorldMixin", "wakes"},
        {"dev.councilcraft.mixin.InventorioSwapHandsMixin", "inventorio"},
        {"dev.councilcraft.mixin.client.ControllerInventorioScreenMixin", "controlify", "inventorio"},
        {"dev.councilcraft.mixin.client.ControllerKeyboardShaderMixin", "controlify"},
        {"dev.councilcraft.mixin.NetherPortalRatioMixin", "immersive_portals"}
    };

    /**
     * Mixins written against one exact build of their mod: mixin class name, mod id, version, and optionally
     * the behaviour that breaks without it. Another build of that mod skips the mixin instead of patching a
     * different method: with a warning, or with an ERROR naming the broken behaviour when one is given.
     */
    private static final String[][] PINNED_MIXINS = {
        // PPM87. Targets a compiler-named lambda (lambda$static$0) in Controlify's ExtraRenderTypes.
        {"dev.councilcraft.mixin.client.ControllerKeyboardShaderMixin", "controlify", "2.0.4+1.20.1-fabric"},
        // CC-5. Targets the hardcoded 8:1 in IntrinsicPortalGeneration's class initializer.
        {"dev.councilcraft.mixin.NetherPortalRatioMixin", "immersive_portals", "5.2.0",
            "1:1 Nether portals (CC-5); nether portals will link 1:8"},
        // PT13. Swaps the supplier of the last uniform1i call in Iris' CommonUniforms.generalCommonUniforms.
        {"dev.councilcraft.mixin.client.IrisPortalFadeUniformMixin", "iris", "1.7.6+mc1.20.1",
            "portal fog fade-in with shaders (PT13); distant terrain will pop in after portal crossings"},
        // PT13. Starts the fade at the end of IP's ClientTeleportationManager.changePlayerDimension.
        {"dev.councilcraft.mixin.client.IpDimensionChangeFadeMixin", "immersive_portals", "5.2.0"}
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
        FabricLoader loader = FabricLoader.getInstance();
        if (!shouldApply(mixinClassName, loader::isModLoaded)) {
            return false;
        }
        Function<String, String> versionOf = id -> loader.getModContainer(id)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse(null);
        if (!pinnedVersionMatches(mixinClassName, versionOf)) {
            String[] pin = pinFor(mixinClassName);
            if (pin.length > 3) {
                LOGGER.error("CouncilCraft hook failed: {}. {} is written for {} {}, but {} is loaded; the hook is skipped."
                    + " Update the mixin for this version.", pin[3], mixinClassName, pin[1], pin[2], versionOf.apply(pin[1]));
            } else {
                LOGGER.warn("CouncilCraft: {} skipped, it is written for another version of its mod.", mixinClassName);
            }
            return false;
        }
        return true;
    }

    /** False only for a version-pinned mixin whose mod is present in another version. */
    static boolean pinnedVersionMatches(String mixinClassName, Function<String, String> versionOf) {
        String[] pinned = pinFor(mixinClassName);
        if (pinned == null) {
            return true;
        }
        String version = versionOf.apply(pinned[1]);
        return version == null || version.equals(pinned[2]);
    }

    /** The {@link #PINNED_MIXINS} row for this mixin, or null. */
    static String[] pinFor(String mixinClassName) {
        for (String[] pinned : PINNED_MIXINS) {
            if (pinned[0].equals(mixinClassName)) {
                return pinned;
            }
        }
        return null;
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
