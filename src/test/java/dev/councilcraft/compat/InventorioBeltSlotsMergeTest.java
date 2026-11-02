package dev.councilcraft.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CC-B23A/AA: player main-storage index helpers used for the Inventorio MAIN sort pass. */
class InventorioBeltSlotsMergeTest {
    @Test
    void playerMainStorageIsNineThroughThirtyFive() {
        assertFalse(InventorioBeltSlots.isPlayerMainStorageInvIndex(8));
        assertTrue(InventorioBeltSlots.isPlayerMainStorageInvIndex(9));
        assertTrue(InventorioBeltSlots.isPlayerMainStorageInvIndex(35));
        assertFalse(InventorioBeltSlots.isPlayerMainStorageInvIndex(36));
        assertEquals(9, InventorioBeltSlots.PLAYER_STORAGE_INV_FIRST);
        assertEquals(35, InventorioBeltSlots.PLAYER_STORAGE_INV_LAST);
    }

    @Test
    void appendSkipsNullArgsAndEmptySlots() {
        List<Object> out = new ArrayList<>();
        InventorioBeltSlots.appendPlayerMainStorageIndices(null, out);
        assertTrue(out.isEmpty());
        InventorioBeltSlots.appendPlayerMainStorageIndices(List.of(), null);
        InventorioBeltSlots.appendPlayerMainStorageIndices(List.of(), out);
        assertTrue(out.isEmpty());
    }

    @Test
    void appendOnlyAddsMissingIntegerIndicesViaPredicate() {
        // Pure path without Minecraft Slot instances: nothing matches → out unchanged.
        List<Object> slots = new ArrayList<>();
        slots.add("not-a-slot");
        slots.add(null);
        List<Object> out = new ArrayList<>();
        out.add(0);
        InventorioBeltSlots.appendPlayerMainStorageIndices(slots, out);
        assertEquals(List.of(0), out);
    }
}
