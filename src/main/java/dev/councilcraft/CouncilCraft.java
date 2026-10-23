package dev.councilcraft;

import dev.councilcraft.handshake.LoginHandshake;
import dev.councilcraft.item.GovernmentMolds;
import dev.councilcraft.item.NetheriteHorseshoes;
import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.world.DifficultyLock;
import dev.councilcraft.world.VillagerRemoval;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CouncilCraft implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(CouncilCraftRules.MOD_ID);

    @Override
    public void onInitialize() {
        LoginHandshake.registerServer();
        NetheriteHorseshoes.register();
        GovernmentMolds.register();
        dev.councilcraft.item.AntiqueAtlasItem.register();
        Reborn.register();
        dev.councilcraft.respawn.RebornFeeding.register();
        VillagerRemoval.register();
        DifficultyLock.register();
        // These classes keep their own names (they are not Minecraft classes).
        // Loading them here, after mixins are registered, applies our mixins and
        // throws if the class was renamed. The mixin json still requires each method.
        // Doing the same check inside the mixin plugin would be too early: the
        // class would load before the mixin was registered, and a @Pseudo mixin
        // would miss it.
        requireClass(
            "fuzs.mutantmonsters.world.level.MutatedExplosion",
            "mutant creeper / minion no block damage (S224)"
        );
        requireClass(
            "com.simibubi.create.content.equipment.toolbox.ToolboxInventory",
            "toolbox whitelist (S234)"
        );
        requireClass(
            "com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity",
            "sprocket and cog minting over diamond blocks"
        );
        requireClass(
            "dev.ithundxr.createnumismatics.content.backend.Coin",
            "coin values (S123)"
        );
        requireClass(
            "dev.ithundxr.createnumismatics.content.coins.CoinItem",
            "coin value tooltip in diamonds (PM32)"
        );
        requireClass(
            "dev.ithundxr.createnumismatics.content.vendor.VendorBlockEntity",
            "vendor price text (PM32a)"
        );
        requireClass(
            "dev.ithundxr.createnumismatics.content.depositor.BrassDepositorBlockEntity",
            "brass depositor price text (PM32a)"
        );
        requireClass(
            "dev.ithundxr.createnumismatics.content.salepoint.SalepointBlockEntity",
            "salepoint price text (PM32a)"
        );
        requireClass(
            "dev.ithundxr.createnumismatics.content.backend.sub_authorization.Limit",
            "spending limit text (PM32a)"
        );
        requireClass(
            "com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity",
            "faster elevators (PM38)"
        );
        requireClass(
            "plus.dragons.createenchantmentindustry.common.processing.forger.BlazeForgerInventory",
            "Blaze Forger clamp (S177)"
        );
        requireClass(
            "plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBlockEntity",
            "printed copies (S207)"
        );
        requireClass(
            "dev.ithundxr.createnumismatics.content.backend.behaviours.SliderStylePriceBehaviour",
            "vendor change-making (PM34)"
        );
        requireClass(
            "io.github.mortuusars.exposure.world.level.storage.ExposureRepository",
            "exposure upload size guard (PM26)"
        );
        if (FabricLoader.getInstance().isModLoaded("carryon") && FabricLoader.getInstance().isModLoaded("supplementaries")) {
            requireClass(
                "tschipp.carryon.CarryOnCommon",
                "Carry On weight as Overencumbered (PPM35)"
            );
        }
        if (FabricLoader.getInstance().isModLoaded("inventorio")) {
            requireClass(
                "net.mehvahdjukaar.supplementaries.common.utils.fabric.ItemsUtilImpl",
                "sack weight in Inventorio slots"
            );
            requireClass(
                "de.rubixdev.inventorio.player.PlayerAddonSerializer",
                "4-slot tool belt and its save migration (PPM68)"
            );
            // PPM68. Old belt swords that did not fit the main inventory drop once the player has joined.
            dev.councilcraft.compat.InventorioToolBelt.register();
        }
        if (FabricLoader.getInstance().isModLoaded("imm_ptl_core") && FabricLoader.getInstance().isModLoaded("surveyor")) {
            requireClass(
                "qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph",
                "Surveyor exploration for chunks sent by Immersive Portals (PPM100)"
            );
            // PPM100. Separate class so Immersive Portals and Surveyor types load only when both mods are present.
            dev.councilcraft.compat.SurveyorPortalExploration.register();
        }
        // PPM102. Donor badges and government roles (server side; the client needs nothing).
        dev.councilcraft.supporter.Supporters.register();
        LOGGER.info("CouncilCraft {} loaded (client and server).", version());
    }

    /**
     * Checks that a hooked class still exists without loading it. Loading it here would run its
     * mixins and static initialiser during onInitialize, before the owning mod may be set up.
     */
    private static void requireClass(String className, String hook) {
        String resource = className.replace('.', '/') + ".class";
        if (CouncilCraft.class.getClassLoader().getResource(resource) == null) {
            throw new IllegalStateException(
                "CouncilCraft hook failed: " + hook + ". " + className + " is missing or was renamed."
            );
        }
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
