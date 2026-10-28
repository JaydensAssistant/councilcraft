package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 0.4.24: unit-testable locks PT87–PT89 without a world. */
class Release0424RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void versionIs0424OrLater() throws IOException {
        assertTrue(read("gradle.properties").matches("(?s).*mod_version=0\\.4\\.(2[4-9]|[3-9]\\d).*"));
        assertTrue(read("pack/pack.toml").matches("(?s).*version = \"0\\.4\\.(2[4-9]|[3-9]\\d)\".*"));
    }

    // PT87 -------------------------------------------------------------------------------------------------------

    @Test
    void complementaryLightShaftMultipliersAre200() throws IOException {
        String settings = read("pack/shaderpacks/ComplementaryUnbound_r5.9.3.zip.txt");
        assertTrue(settings.contains("LIGHTSHAFT_NOON_I=200"));
        assertTrue(settings.contains("LIGHTSHAFT_NIGHT_I=200"));
        assertTrue(settings.contains("LIGHTSHAFT_SUNSET_I=200"));
        assertTrue(settings.contains("LIGHTSHAFT_RAIN_I=200"));
        // Prior pack defaults kept.
        assertTrue(settings.contains("LIGHTSHAFT_SMOKE=true"));
        assertTrue(settings.contains("shadowDistance=128.0"));
    }

    // PT88 -------------------------------------------------------------------------------------------------------

    @Test
    void ipnFillSlotsUsesWrapMethodNotReturnInject() throws IOException {
        String m = read("src/main/java/dev/councilcraft/mixin/client/IpnItemStorageBeltMixin.java");
        assertTrue(m.contains("WrapMethod"));
        assertTrue(m.contains("com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod"));
        assertTrue(m.contains("Operation<Void>"));
        assertTrue(m.contains("original.call(types, slots, out)"));
        assertTrue(m.contains("fillSlots"));
        assertTrue(m.contains("InventorioBeltSlots.isExcludedFromContainerSort"));
        assertTrue(m.contains("targets = \"org.anti_ad.mc.ipnext.inventory.AreaTypes\""));
        // Must not use the broken RETURN inject that reloads overwritten locals.
        assertFalse(m.contains("@Inject"));
        assertFalse(m.contains("@At(\"RETURN\")") || m.contains("@At(value = \"RETURN\")"));
        assertFalse(m.contains("CallbackInfo"));
        assertFalse(m.contains("removeIf"));
        String client = read("src/main/resources/councilcraft.client.mixins.json");
        assertTrue(client.contains("IpnItemStorageBeltMixin"));
        assertTrue(client.contains("IpnDisabledBeltMixin"));
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        assertTrue(plugin.contains("IpnItemStorageBeltMixin"));
        assertTrue(plugin.contains("IpnDisabledBeltMixin"));
        assertTrue(plugin.contains("\"1.10.20\""));
    }

    // PT89 -------------------------------------------------------------------------------------------------------

    @Test
    void inventorioMoveToPlayerIsHidden() throws IOException {
        String hints = read(
            "pack/config/yosbr/config/inventoryprofilesnext/integrationHints/councilcraft.json");
        assertTrue(hints.contains("de.rubixdev.inventorio.client.ui.InventorioScreen"));
        assertTrue(hints.contains("MOVE_TO_PLAYER"));
        assertTrue(hints.contains("MOVE_TO_CONTAINER"));
        // PT89: MOVE_TO_PLAYER stays hidden. PT90: MOVE_TO_CONTAINER is shown with new offsets.
        assertTrue(hints.contains("\"hide\": true") || hints.contains("\"hide\":true"));
        assertTrue(hints.contains("SORT"));
        String mixin = read("src/main/java/dev/councilcraft/mixin/client/IpnHonorHideMixin.java");
        assertTrue(mixin.contains("moveAllToPlayer"));
        assertTrue(mixin.contains("moveAllToContainer"));
        assertTrue(mixin.contains("getHide"));
        assertTrue(mixin.contains("setVisible"));
    }
}
