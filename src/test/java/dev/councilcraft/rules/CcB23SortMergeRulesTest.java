package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.compat.client.IpnInventorioSortMerge;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * CC-B23A / CC-B23AA: Inventorio Sort runs Deep Pockets and main inventory separately; nothing
 * crosses; no-pockets still sorts main; chests unchanged.
 */
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
        assertTrue(plugin.contains("CC-B23A/AA"));
    }

    @Test
    void sortEntryWrapsDoSortWithSeparatePasses() throws IOException {
        String m = read("src/main/java/dev/councilcraft/mixin/client/IpnInventorioSortMergeMixin.java");
        assertTrue(m.contains("GeneralInventoryActions"));
        assertTrue(m.contains("doSort"));
        assertTrue(m.contains("doSortInColumns"));
        assertTrue(m.contains("doSortInRows"));
        assertTrue(m.contains("WrapMethod"));
        assertTrue(m.contains("IpnInventorioSortMerge.runSeparatePlayerSorts"));
        assertFalse(m.contains("runMergingPlayerStorage"));
        assertFalse(m.contains("AreaTypes$sortableItemStorage"));
        assertFalse(m.contains("AreaTypes$itemStorage"));
    }

    @Test
    void fillSlotsAppliesPassFilterNotMergeAppend() throws IOException {
        String m = read("src/main/java/dev/councilcraft/mixin/client/IpnItemStorageBeltMixin.java");
        assertTrue(m.contains("IpnInventorioSortMerge.applySortPassSlotFilter"));
        assertFalse(m.contains("appendPlayerStorageIfMerging"));
        assertTrue(m.contains("InventorioBeltSlots.isExcludedFromContainerSort"));
        String helper = read("src/main/java/dev/councilcraft/compat/client/IpnInventorioSortMerge.java");
        assertTrue(helper.contains("ThreadLocal"));
        assertTrue(helper.contains("Pass.DEEP_POCKETS"));
        assertTrue(helper.contains("Pass.MAIN"));
        assertTrue(helper.contains("runSeparatePlayerSorts"));
        assertTrue(helper.contains("out.clear()"));
        assertTrue(helper.contains("appendPlayerMainStorageIndices"));
        String belt = read("src/main/java/dev/councilcraft/compat/InventorioBeltSlots.java");
        assertTrue(belt.contains("PLAYER_STORAGE_INV_FIRST = 9"));
        assertTrue(belt.contains("PLAYER_STORAGE_INV_LAST = 35"));
        assertTrue(belt.contains("appendPlayerMainStorageIndices"));
        assertTrue(belt.contains("isInventorioScreenHandler"));
    }

    @Test
    void separateSortsRunDeepPocketsThenMain() {
        List<IpnInventorioSortMerge.Pass> seen = new ArrayList<>();
        IpnInventorioSortMerge.runBothPassesForTest(() -> seen.add(IpnInventorioSortMerge.currentPass()));
        assertEquals(
            List.of(IpnInventorioSortMerge.Pass.DEEP_POCKETS, IpnInventorioSortMerge.Pass.MAIN),
            seen);
        assertEquals(IpnInventorioSortMerge.Pass.OFF, IpnInventorioSortMerge.currentPass());
    }

    @Test
    void nothingCrosses_mainPassClearsPocketIndices_deepPassLeavesThem() {
        // DEEP_POCKETS: pocket indices already in out stay (no append of main without Slot mocks).
        IpnInventorioSortMerge.runWithPassForTest(IpnInventorioSortMerge.Pass.DEEP_POCKETS, () -> {
            List<Object> out = new ArrayList<>(List.of(10, 11, 12));
            IpnInventorioSortMerge.applySortPassSlotFilter(List.of(), out);
            assertEquals(List.of(10, 11, 12), out);
        });
        // MAIN: prior pocket indices wiped so the sort set cannot include both areas.
        IpnInventorioSortMerge.runWithPassForTest(IpnInventorioSortMerge.Pass.MAIN, () -> {
            List<Object> out = new ArrayList<>(List.of(10, 11, 12));
            IpnInventorioSortMerge.applySortPassSlotFilter(List.of(), out);
            assertTrue(out.isEmpty(), "MAIN must clear pocket/container indices before appending main");
        });
    }

    @Test
    void noPocketsStillSortsMain_emptyDeepPassFallsThrough_mainPassStillRuns() {
        // Empty DEEP_POCKETS out → IPN falls back to playerStorage (CC-92); MAIN still runs after.
        AtomicInteger passes = new AtomicInteger();
        IpnInventorioSortMerge.runBothPassesForTest(() -> {
            passes.incrementAndGet();
            if (IpnInventorioSortMerge.currentPass() == IpnInventorioSortMerge.Pass.DEEP_POCKETS) {
                List<Object> out = new ArrayList<>();
                IpnInventorioSortMerge.applySortPassSlotFilter(List.of(), out);
                assertTrue(out.isEmpty(), "no unlocked pockets → empty sortableItemStorage");
            }
            if (IpnInventorioSortMerge.currentPass() == IpnInventorioSortMerge.Pass.MAIN) {
                List<Object> out = new ArrayList<>(List.of(99));
                IpnInventorioSortMerge.applySortPassSlotFilter(List.of(), out);
                assertTrue(out.isEmpty() || out.stream().noneMatch(i -> Integer.valueOf(99).equals(i)));
            }
        });
        assertEquals(2, passes.get());
    }

    @Test
    void chestUnchanged_nonInventorioSortsOnce() {
        AtomicInteger n = new AtomicInteger();
        IpnInventorioSortMerge.runSeparatePlayerSorts(null, n::incrementAndGet);
        assertEquals(1, n.get());
        assertEquals(IpnInventorioSortMerge.Pass.OFF, IpnInventorioSortMerge.currentPass());
    }

    @Test
    void inventorioHintsDoNotForcePlayerSideOnly() throws IOException {
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
