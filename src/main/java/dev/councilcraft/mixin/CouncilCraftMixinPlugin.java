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
        {"dev.councilcraft.mixin.NetherPortalRatioMixin", "immersive_portals"},
        {"dev.councilcraft.mixin.DciLeaveGuardMixin", "dcintegration-fabric"},
        {"dev.councilcraft.mixin.DciCommandGuardMixin", "dcintegration-fabric"},
        {"dev.councilcraft.mixin.client.CarriedHeadJitterMixin", "carryon"},
        {"dev.councilcraft.mixin.client.CarriedFirstPersonJitterMixin", "carryon"},
        {"dev.councilcraft.mixin.CarryEscapeMixin", "carryon"},
        {"dev.councilcraft.mixin.client.PunchyAlbumEquipMixin", "punchy"},
        {"dev.councilcraft.mixin.ShipFlagBannerMixin", "smallships"},
        {"dev.councilcraft.mixin.BannerableFlagMixin", "smallships"},
        {"dev.councilcraft.mixin.ShipDetachFlagMixin", "smallships"},
        {"dev.councilcraft.mixin.DockyardFlagMixin", "smallships"},
        {"dev.councilcraft.mixin.InventorioCraftingQuickMoveMixin", "inventorio"},
        {"dev.councilcraft.mixin.UtilityBeltSwapMixin", "inventorio"},
        {"dev.councilcraft.mixin.InventorioKnockoutOpenMixin", "inventorio", "hardcorerevival"},
        {"dev.councilcraft.mixin.client.KnockoutInventoryGuardMixin", "hardcorerevival"},
        {"dev.councilcraft.mixin.client.FeedUseActionMixin", "punchy"},
        {"dev.councilcraft.mixin.client.BetterThirdPersonControllerMixin", "betterthirdperson"},
        {"dev.councilcraft.mixin.client.PunchySwimCarryMixin", "punchy", "carryon"},
        {"dev.councilcraft.mixin.client.IpnDisabledBeltMixin", "inventoryprofilesnext", "inventorio"},
        {"dev.councilcraft.mixin.client.IpnItemStorageBeltMixin", "inventoryprofilesnext", "inventorio"},
        {"dev.councilcraft.mixin.client.IpnHonorHideMixin", "inventoryprofilesnext"},
        {"dev.councilcraft.mixin.client.MouseTweaksDeepPocketsMixin", "mousetweaks", "inventorio"},
        {"dev.councilcraft.mixin.client.DockyardScreenFlagMixin", "smallships"}
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
        {"dev.councilcraft.mixin.client.IpDimensionChangeFadeMixin", "immersive_portals", "5.2.0"},
        // PT14. Text-only album pages: Exposure's AlbumScreen, PhotographSlotWidget and AlbumViewScreen internals.
        {"dev.councilcraft.mixin.client.AlbumTextPageMixin", "exposure", "1.9.22",
            "text-only album pages (PT14); album notes keep Exposure's 3-line layout"},
        {"dev.councilcraft.mixin.client.AlbumPhotoSlotMixin", "exposure", "1.9.22",
            "album frame fade and hint (PT14); empty album frames show no hint"},
        {"dev.councilcraft.mixin.client.AlbumViewTextPageMixin", "exposure", "1.9.22",
            "full-page notes in signed albums (PT14); note-only pages show Exposure's layout"},
        // PT18. Raises DCI's own "stopped" flag around its leave hook, and targets lambda$new$2 in McCommandDiscord.
        {"dev.councilcraft.mixin.DciLeaveGuardMixin", "dcintegration-fabric", "3.0.7",
            "Discord Integration guard (PT18); without a running bot, leaving players will crash the server"},
        {"dev.councilcraft.mixin.DciCommandGuardMixin", "dcintegration-fabric", "3.0.7",
            "Discord Integration guard (PT18); without a running bot, joining players will be kicked"},
        // PT23. Carry On's CarryRenderHelper / CarriedObjectRender internals (as upstream PR #997).
        {"dev.councilcraft.mixin.client.CarriedHeadJitterMixin", "carryon", "2.1.2.7"},
        {"dev.councilcraft.mixin.client.CarriedFirstPersonJitterMixin", "carryon", "2.1.2.7"},
        // PT17. Punchy's private HandEquipStateMachine.hasItemChanged(ItemStack, ItemStack).
        {"dev.councilcraft.mixin.client.PunchyAlbumEquipMixin", "punchy", "2.8g"},
        // PT43. Supplementaries flags on Small Ships: one pin row per mod, and every row must match. The mixins
        // hook Ship.setData, Bannerable.interactBanner, ServerboundShipDetachPacket.giveBack, DockyardBlockEntity
        // startModifyTask / finishModify and DockyardScreen addBannerOptions / buildOptionSignature.
        {"dev.councilcraft.mixin.ShipFlagBannerMixin", "smallships", "2.0.1",
            "Supplementaries flags on Small Ships (PT43); flags will not be accepted as ship banners"},
        {"dev.councilcraft.mixin.ShipFlagBannerMixin", "supplementaries", "1.20-3.1.44",
            "Supplementaries flags on Small Ships (PT43); flags will not be accepted as ship banners"},
        {"dev.councilcraft.mixin.BannerableFlagMixin", "smallships", "2.0.1",
            "Supplementaries flags on Small Ships (PT43); flags will not be accepted as ship banners"},
        {"dev.councilcraft.mixin.BannerableFlagMixin", "supplementaries", "1.20-3.1.44",
            "Supplementaries flags on Small Ships (PT43); flags will not be accepted as ship banners"},
        {"dev.councilcraft.mixin.ShipDetachFlagMixin", "smallships", "2.0.1",
            "Supplementaries flags on Small Ships (PT43); flags will not be accepted as ship banners"},
        {"dev.councilcraft.mixin.ShipDetachFlagMixin", "supplementaries", "1.20-3.1.44",
            "Supplementaries flags on Small Ships (PT43); flags will not be accepted as ship banners"},
        {"dev.councilcraft.mixin.DockyardFlagMixin", "smallships", "2.0.1",
            "Supplementaries flags on Small Ships (PT43); flags will not be accepted as ship banners"},
        {"dev.councilcraft.mixin.DockyardFlagMixin", "supplementaries", "1.20-3.1.44",
            "Supplementaries flags on Small Ships (PT43); flags will not be accepted as ship banners"},
        {"dev.councilcraft.mixin.client.DockyardScreenFlagMixin", "smallships", "2.0.1",
            "Supplementaries flags in the Small Ships Dockyard (PT43); flags will not be listed as banner choices"},
        {"dev.councilcraft.mixin.client.DockyardScreenFlagMixin", "supplementaries", "1.20-3.1.44",
            "Supplementaries flags in the Small Ships Dockyard (PT43); flags will not be listed as banner choices"},
        // PT66. Better Third Person ClientAdapter.isCameraOnPlayer.
        {"dev.councilcraft.mixin.client.BetterThirdPersonControllerMixin", "betterthirdperson", "1.9.0",
            "BTP camera off on controller (PT66); controller third person may use BTP camera"},
        // PT70. Punchy SwimmingStateMachine.tick while carrying.
        {"dev.councilcraft.mixin.client.PunchySwimCarryMixin", "punchy", "2.8g",
            "no Punchy swim while carrying (PT70); swim strokes may play while carrying"},
        // PT76 / PT85. IPN AreaTypes disabled$1 / fillSlots.
        {"dev.councilcraft.mixin.client.IpnDisabledBeltMixin", "inventoryprofilesnext", "1.10.20",
            "IPN skips Inventorio belts (PT76); sort may touch tool/utility belt"},
        {"dev.councilcraft.mixin.client.IpnItemStorageBeltMixin", "inventoryprofilesnext", "1.10.20",
            "IPN skips Inventorio belts on container sort (PT76/PT85); sort may touch belts"},
        // PT86. InitWidgets honors ButtonPositionHint.hide for move-all.
        {"dev.councilcraft.mixin.client.IpnHonorHideMixin", "inventoryprofilesnext", "1.10.20",
            "IPN honors hide for move-all buttons (PT86); Inventorio may show orphan move-matching"},
        // PT83. Mouse Tweaks findPushSlots / findPullSlot Deep Pockets filter.
        {"dev.councilcraft.mixin.client.MouseTweaksDeepPocketsMixin", "mousetweaks", "2.26",
            "Mouse Tweaks Deep Pockets scroll (PT83); scroll may miss Deep Pockets"},
        {"dev.councilcraft.mixin.client.MouseTweaksDeepPocketsMixin", "inventorio", "1.10.4",
            "Mouse Tweaks Deep Pockets scroll (PT83); scroll may miss Deep Pockets"},
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
            String[] pin = mismatchedPin(mixinClassName, versionOf);
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
        return mismatchedPin(mixinClassName, versionOf) == null;
    }

    /** The first {@link #PINNED_MIXINS} row of this mixin whose mod is present in another version, or null. */
    static String[] mismatchedPin(String mixinClassName, Function<String, String> versionOf) {
        for (String[] pinned : PINNED_MIXINS) {
            if (pinned[0].equals(mixinClassName)) {
                String version = versionOf.apply(pinned[1]);
                if (version != null && !version.equals(pinned[2])) {
                    return pinned;
                }
            }
        }
        return null;
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
