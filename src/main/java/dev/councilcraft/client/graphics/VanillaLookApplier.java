package dev.councilcraft.client.graphics;

import dev.councilcraft.CouncilCraft;
import dev.councilcraft.rules.VanillaLook;
import java.util.ArrayList;
import java.util.List;
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

    /** Turns the Vanilla look on or off, saves options.txt and reloads resources if the pack list changed. */
    public static void set(MinecraftClient client, boolean on) {
        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks();
        List<String> enabled = new ArrayList<>(manager.getEnabledNames());
        List<String> wanted = VanillaLook.apply(enabled, on, name -> switchable(manager, name));
        manager.setEnabledProfiles(wanted);
        client.options.refreshResourcePacks(manager);
        CouncilCraft.LOGGER.info("CouncilCraft vanilla look {}", on ? "on" : "off");
    }
}
