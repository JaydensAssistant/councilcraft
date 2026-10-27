package dev.councilcraft.compat;

import de.rubixdev.inventorio.slot.DeepPocketsSlot;
import de.rubixdev.inventorio.slot.ToolBeltSlot;
import de.rubixdev.inventorio.util.GeneralConstants;
import kotlin.ranges.IntRange;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.screen.slot.Slot;

/**
 * PT76 / PT83. Inventorio belt and crafting slots that IPN / Mouse Tweaks must not treat as
 * sortable or scroll targets. Utility-belt slots are also {@link DeepPocketsSlot}; filter by
 * addon index, not class.
 */
public final class InventorioBeltSlots {
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
        if (slot instanceof DeepPocketsSlot) {
            int idx = slot.getIndex();
            IntRange util = GeneralConstants.INVENTORY_ADDON_UTILITY_BELT_RANGE;
            return idx >= util.getFirst() && idx <= util.getLast();
        }
        return false;
    }

    /** True when this handler slot index is inside Inventorio's available Deep Pockets rows. */
    public static boolean inDeepPocketsRange(int handlerIndex, IntRange availableDeepPockets) {
        return availableDeepPockets != null
            && handlerIndex >= availableDeepPockets.getFirst()
            && handlerIndex <= availableDeepPockets.getLast();
    }
}
