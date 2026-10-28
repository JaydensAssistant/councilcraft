package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 0.4.23: unit-testable locks PT84–PT86 without a world. */
class Release0423RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    private static boolean exists(String p) {
        return Files.exists(Path.of(p));
    }

    @Test
    void versionIs0423OrLater() throws IOException {
        assertTrue(read("gradle.properties").matches("(?s).*mod_version=0\\.4\\.(2[3-9]|[3-9]\\d).*"));
        assertTrue(read("pack/pack.toml").matches("(?s).*version = \"0\\.4\\.(2[3-9]|[3-9]\\d)\".*"));
    }

    // PT84 -------------------------------------------------------------------------------------------------------

    @Test
    void toastBegoneIsClientModAtPinnedVersion() throws IOException {
        assertTrue(exists("pack/mods/toastbegone.pw.toml"));
        String toml = read("pack/mods/toastbegone.pw.toml");
        assertTrue(toml.contains("side = \"client\""));
        assertTrue(toml.contains("toastbegone-fabric-1.20-1.2.0.jar"));
        assertTrue(toml.contains("ixnLpbxR"));
        assertTrue(toml.contains("g8reZf3x"));
        assertTrue(toml.contains(
            "04414d16b37c45b3f392f559867b7e0a172fc157fceb9bc1de6c58916d670d6d698416659d43c759a8573ed0eb3cae7ebd5ad64ef54ddd6c1acc3041d1e7d088"));
        // Defaults only — no shipped override config that would change blockAdvancement / belt hint.
        assertFalse(exists("pack/config/toastbegone.json"));
        assertFalse(exists("pack/config/yosbr/config/toastbegone.json"));
        assertTrue(read("pack/credits.txt").contains("Toast Begone"));
    }

    // PT85 -------------------------------------------------------------------------------------------------------

    @Test
    void ipnBeltFilterTargetsFillSlotsNotItemStorageClinit() throws IOException {
        String m = read("src/main/java/dev/councilcraft/mixin/client/IpnItemStorageBeltMixin.java");
        assertTrue(m.contains("AreaTypes"));
        assertTrue(m.contains("fillSlots"));
        assertFalse(m.contains("targets = \"org.anti_ad.mc.ipnext.inventory.AreaTypes$itemStorage$1\""));
        assertTrue(m.contains("targets = \"org.anti_ad.mc.ipnext.inventory.AreaTypes\""));
        assertTrue(m.contains("InventorioBeltSlots.isExcludedFromContainerSort"));
        // Plain loop — no removeIf / lambda on the out list.
        assertFalse(m.contains("removeIf"));
        String client = read("src/main/resources/councilcraft.client.mixins.json");
        assertTrue(client.contains("IpnItemStorageBeltMixin"));
        assertTrue(client.contains("IpnDisabledBeltMixin"));
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        assertTrue(plugin.contains("IpnItemStorageBeltMixin"));
        assertTrue(plugin.contains("IpnDisabledBeltMixin"));
        assertTrue(plugin.contains("\"1.10.20\""));
    }

    // PT86 -------------------------------------------------------------------------------------------------------

    @Test
    void inventorioHonorHideMixinStillPresentAndGlobalMoveAllOn() throws IOException {
        // PT86 mixin kept; PT90 un-hides MOVE_TO_CONTAINER on Inventorio (MOVE_TO_PLAYER stays hidden).
        String hints = read(
            "pack/config/yosbr/config/inventoryprofilesnext/integrationHints/councilcraft.json");
        assertTrue(hints.contains("de.rubixdev.inventorio.client.ui.InventorioScreen"));
        assertTrue(hints.contains("MOVE_TO_CONTAINER"));
        assertTrue(hints.contains("MOVE_TO_PLAYER"));
        assertTrue(hints.contains("SORT"));
        String mixin = read("src/main/java/dev/councilcraft/mixin/client/IpnHonorHideMixin.java");
        assertTrue(mixin.contains("SortingButtonCollectionWidget$InitWidgets"));
        assertTrue(mixin.contains("moveAllToContainer"));
        assertTrue(mixin.contains("moveAllToPlayer"));
        assertTrue(mixin.contains("getHide"));
        assertTrue(mixin.contains("setVisible"));
        String client = read("src/main/resources/councilcraft.client.mixins.json");
        assertTrue(client.contains("IpnHonorHideMixin"));
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        assertTrue(plugin.contains("IpnHonorHideMixin"));
        // Global SHOW_MOVE_ALL_BUTTON stays on.
        String ipn = read("pack/config/yosbr/config/inventoryprofilesnext/inventoryprofiles.json");
        assertFalse(ipn.toLowerCase().contains("\"show_move_all_button\": false")
            || ipn.toLowerCase().contains("\"show_move_all_button\":false"));
    }
}
