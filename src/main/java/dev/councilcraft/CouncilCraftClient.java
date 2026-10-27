package dev.councilcraft;

import dev.councilcraft.compat.client.HorseshoeTooltip;
import dev.councilcraft.handshake.LoginHandshake;
import dev.councilcraft.respawn.RebornClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client half of the login handshake, and the Reborn active-time packet.
 * Dedicated servers do not load this class.
 */
public final class CouncilCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LoginHandshake.registerClient();
        RebornClient.register();
        dev.councilcraft.respawn.RebornFeedingClient.register();
        HorseshoeTooltip.register();
        dev.councilcraft.print.CopyCostTooltip.register();
        // PT49. Printed copies show only "Printed Copy", no generation line.
        dev.councilcraft.print.PrintedTooltipFilter.register();
        // PT36/PT46. CMYK Ink: its own still/flowing CMYK-band textures, untinted, flat and fully opaque (solid layer). The bucket has
        // its own CMYK texture (no tint).
        net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry.INSTANCE.register(
            dev.councilcraft.print.ProcessInk.STILL,
            dev.councilcraft.print.ProcessInk.FLOWING,
            new net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler(
                new net.minecraft.util.Identifier("councilcraft", "block/process_ink_still"),
                new net.minecraft.util.Identifier("councilcraft", "block/process_ink_flow"))
        );
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putFluids(
            net.minecraft.client.render.RenderLayer.getSolid(),
            dev.councilcraft.print.ProcessInk.STILL, dev.councilcraft.print.ProcessInk.FLOWING);
        // PPPM3. Zebras render horse armor.
        dev.councilcraft.compat.client.ZebraArmorFeatureRenderer.register();
        // PPM61. Graphics preset picker (first title screen) and the Video Settings button.
        dev.councilcraft.client.graphics.GraphicsPresets.register();
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("punchy")) {
            // PPM101. One-time sync: a player who already has the Vanilla look on gets Punchy turned off. At
            // CLIENT_STARTED, after Punchy has loaded its config and the resource packs are known.
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
                if (dev.councilcraft.client.graphics.VanillaLookApplier.isOn(client)) {
                    dev.councilcraft.client.graphics.VanillaLookApplier.syncPunchy(true);
                }
            });
        }
        if (loader.isModLoaded("inventorio")) {
            // PPM67. Hold Left Alt (rebindable) and scroll to cycle the utility belt.
            dev.councilcraft.client.input.UtilityBeltScroll.register();
            // PPM88. Belt switch hints: one-time toast and empty-slot tooltip in Inventorio's screen.
            dev.councilcraft.client.input.UtilityBeltHints.register();
        }
        if (loader.isModLoaded("distanthorizons") && loader.isModLoaded("imm_ptl_core")) {
            // PM21. Separate class so DH and IP types load only when both mods are present.
            dev.councilcraft.compat.DhPortalDeferredFix.register();
            // PT13. Portal fog fade-in: DH fog without shaders, and the per-tick dimension change fallback.
            dev.councilcraft.compat.PortalFadeDhFog.register();
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                dev.councilcraft.compat.client.PortalFade::onEndTick);
        }
        if (loader.isModLoaded("create") && loader.isModLoaded("ponder")) {
            // PT36 (d). CMYK Ink Ponder scene. Separate class so Ponder types load only when Ponder is present.
            dev.councilcraft.print.ponder.CmykInkPonderPlugin.register();
        }
        if (loader.isModLoaded("wakes")) {
            // PPM80 (B). Separate class so Wakes types load only when Wakes is present.
            dev.councilcraft.client.compat.WakesWorldSync.register();
        }
    }
}
