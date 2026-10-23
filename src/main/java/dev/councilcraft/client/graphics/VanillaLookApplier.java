package dev.councilcraft.client.graphics;

import dev.councilcraft.CouncilCraft;
import dev.councilcraft.rules.VanillaLook;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

/** PPM78. Reads and switches the Vanilla look on the client's resource pack manager. */
public final class VanillaLookApplier {
    private VanillaLookApplier() {}

    private static boolean switchable(ResourcePackManager manager, String name) {
        ResourcePackProfile profile = manager.getProfile(name);
        return profile != null && !profile.isAlwaysEnabled();
    }

    public static boolean isOn(MinecraftClient client) {
        ResourcePackManager manager = client.getResourcePackManager();
        return VanillaLook.isOn(manager.getEnabledNames(), name -> switchable(manager, name));
    }

    /** Turns the Vanilla look on or off (and Punchy the other way), saves options.txt and reloads resources. */
    public static void set(MinecraftClient client, boolean on) {
        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks();
        List<String> enabled = new ArrayList<>(manager.getEnabledNames());
        List<String> wanted = VanillaLook.apply(enabled, on, name -> switchable(manager, name));
        manager.setEnabledProfiles(wanted);
        // PPM101. Saved before the reload, because Punchy re-reads its config file when resources reload.
        syncPunchy(on);
        client.options.refreshResourcePacks(manager);
        CouncilCraft.LOGGER.info("CouncilCraft vanilla look {}", on ? "on" : "off");
    }

    /** PPM101. Punchy is off while the Vanilla look is on, and on while it is off. */
    public static void syncPunchy(boolean vanillaLookOn) {
        if (FabricLoader.getInstance().isModLoaded("punchy")) {
            dev.councilcraft.client.compat.PunchyVanillaLook.apply(vanillaLookOn);
        }
    }
}
