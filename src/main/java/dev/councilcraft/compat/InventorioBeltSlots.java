package dev.councilcraft.compat;

import de.rubixdev.inventorio.player.InventorioScreenHandler;
import de.rubixdev.inventorio.slot.DeepPocketsSlot;
import de.rubixdev.inventorio.slot.ToolBeltSlot;
import de.rubixdev.inventorio.util.GeneralConstants;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kotlin.ranges.IntRange;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * PT76 / PT83. Inventorio belt and crafting slots that IPN / Mouse Tweaks must not treat as
 * sortable or scroll targets. Utility-belt slots are also {@link DeepPocketsSlot}; filter by
 * addon index, not class.
 */
public final class InventorioBeltSlots {
    /** IPN PlayerSlotIds.storageInvSlots — main inventory rows, not hotbar. */
    public static final int PLAYER_STORAGE_INV_FIRST = 9;
    public static final int PLAYER_STORAGE_INV_LAST = 35;

    private InventorioBeltSlots() {
    }

    /** Tool belt, utility belt, and the 2x2 crafting grid / result — never container-sorted. */
    public static boolean isExcludedFromContainerSort(Slot slot) {
        if (slot == null) {
            return false;
        }
        if (slot.inventory instanceof CraftingInventory || slot.inventory instanceof CraftingResultInventory) {
            return true;
        }
        if (slot instanceof ToolBeltSlot) {
            return true;
        }
        if (slot instanceof DeepPocketsSlot deep) {
            int idx = slot.getIndex();
            IntRange util = GeneralConstants.INVENTORY_ADDON_UTILITY_BELT_RANGE;
            if (idx >= util.getFirst() && idx <= util.getLast()) {
                return true;
            }
            // CC-92: locked Deep Pockets rows are empty disabled slots that IPN
            // treats as the "container" area on Inventorio's own screen. Drop
            // them so Sort falls back to the player inventory. Unlocked rows
            // stay sortable (PT76).
            if (!deep.isEnabled() || !deep.canTakeItems) {
                return true;
            }
        }
        return false;
    }

    /** True when this handler slot index is inside Inventorio's available Deep Pockets rows. */
    public static boolean inDeepPocketsRange(int handlerIndex, IntRange availableDeepPockets) {
        return availableDeepPockets != null
            && handlerIndex >= availableDeepPockets.getFirst()
            && handlerIndex <= availableDeepPockets.getLast();
    }

    /** IPN main-inventory storage: PlayerInventory indices 9–35 (not hotbar 0–8). */
    public static boolean isPlayerMainStorageInvIndex(int invIndex) {
        return invIndex >= PLAYER_STORAGE_INV_FIRST && invIndex <= PLAYER_STORAGE_INV_LAST;
    }

    public static boolean isInventorioScreenHandler(ScreenHandler handler) {
        return handler instanceof InventorioScreenHandler;
    }

    /**
     * Append handler indices whose slot is vanilla main inventory (PlayerInventory 9–35).
     * Used by CC-B23A/AA MAIN sort pass so Inventorio Sort covers PlayerInventory 9–35 only.
     */
    public static void appendPlayerMainStorageIndices(List<?> slots, List<?> out) {
        if (slots == null || out == null) {
            return;
        }
        Set<Integer> already = new HashSet<>();
        for (Object raw : out) {
            if (raw instanceof Number) {
                already.add(((Number) raw).intValue());
            }
        }
        @SuppressWarnings("unchecked")
        List<Object> mutable = (List<Object>) out;
        int size = slots.size();
        for (int i = 0; i < size; i++) {
            if (already.contains(i)) {
                continue;
            }
            Object slotObj = slots.get(i);
            if (!(slotObj instanceof Slot slot)) {
                continue;
            }
            if (!(slot.inventory instanceof PlayerInventory)) {
                continue;
            }
            if (!isPlayerMainStorageInvIndex(slot.getIndex())) {
                continue;
            }
            mutable.add(i);
            already.add(i);
        }
    }
}
