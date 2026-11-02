package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** CC-B23 wiring: Inventorio Sort merges main inventory into Deep Pockets sort area. */
class CcB23SortMergeRulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void sortMergeMixinRegisteredAndPinned() throws IOException {
        String client = read("src/main/resources/councilcraft.client.mixins.json");
        assertTrue(client.contains("IpnInventorioSortMergeMixin"));
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        assertTrue(plugin.contains("IpnInventorioSortMergeMixin"));
        assertTrue(plugin.contains("\"1.10.20\""));
        assertTrue(plugin.contains("Inventorio Sort merges main + Deep Pockets (CC-B23)"));
    }

    @Test
    void sortEntryWrapsDoSortNotNestedAreaType() throws IOException {
        String m = read("src/main/java/dev/councilcraft/mixin/client/IpnInventorioSortMergeMixin.java");
        assertTrue(m.contains("GeneralInventoryActions"));
        assertTrue(m.contains("doSort"));
        assertTrue(m.contains("doSortInColumns"));
        assertTrue(m.contains("doSortInRows"));
        assertTrue(m.contains("WrapMethod"));
        assertTrue(m.contains("IpnInventorioSortMerge.runMergingPlayerStorage"));
        assertFalse(m.contains("AreaTypes$sortableItemStorage"));
        assertFalse(m.contains("AreaTypes$itemStorage"));
    }

    @Test
    void fillSlotsAppendsPlayerStorageWhileMerging() throws IOException {
        String m = read("src/main/java/dev/councilcraft/mixin/client/IpnItemStorageBeltMixin.java");
        assertTrue(m.contains("IpnInventorioSortMerge.appendPlayerStorageIfMerging"));
        assertTrue(m.contains("InventorioBeltSlots.isExcludedFromContainerSort"));
        String helper = read("src/main/java/dev/councilcraft/compat/client/IpnInventorioSortMerge.java");
        assertTrue(helper.contains("ThreadLocal"));
        assertTrue(helper.contains("appendPlayerMainStorageIndices"));
        String belt = read("src/main/java/dev/councilcraft/compat/InventorioBeltSlots.java");
        assertTrue(belt.contains("PLAYER_STORAGE_INV_FIRST = 9"));
        assertTrue(belt.contains("PLAYER_STORAGE_INV_LAST = 35"));
        assertTrue(belt.contains("appendPlayerMainStorageIndices"));
        assertTrue(belt.contains("isInventorioScreenHandler"));
    }

    @Test
    void inventorioHintsDoNotForcePlayerSideOnly() throws IOException {
        // Config cannot merge areas; playerSideOnly on Inventorio would drop Deep Pockets.
        String hints = read(
            "pack/config/yosbr/config/inventoryprofilesnext/integrationHints/councilcraft.json");
        assertTrue(hints.contains("de.rubixdev.inventorio.client.ui.InventorioScreen"));
        assertTrue(hints.contains("SORT"));
        int inventorio = hints.indexOf("de.rubixdev.inventorio.client.ui.InventorioScreen");
        int nextScreen = hints.indexOf("\n  \"", inventorio + 10);
        String block = nextScreen < 0 ? hints.substring(inventorio) : hints.substring(inventorio, nextScreen);
        assertFalse(block.contains("playerSideOnly"));
    }
}
