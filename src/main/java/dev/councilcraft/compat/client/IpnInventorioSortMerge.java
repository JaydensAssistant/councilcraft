package dev.councilcraft.compat.client;

import dev.councilcraft.compat.InventorioBeltSlots;
import java.util.List;
import net.minecraft.screen.ScreenHandler;

/**
 * CC-B23 / CC-92. While IPN is sorting an Inventorio player screen, mark the sort so
 * {@code fillSlots} can also include vanilla main-inventory indices. Without that, unlocked
 * Deep Pockets alone make {@code sortableItemStorage} non-empty and Sort never touches the
 * survival inventory.
 *
 * <p>Only Sort entry points set the flag — Move Matching still uses {@code itemStorage} /
 * {@code fillSlots} without the merge, so Deep Pockets stay the container target (PT90).
 */
public final class IpnInventorioSortMerge {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private IpnInventorioSortMerge() {
    }

    public static void runMergingPlayerStorage(ScreenHandler handler, Runnable sort) {
        boolean merge = InventorioBeltSlots.isInventorioScreenHandler(handler);
        if (merge) {
            ACTIVE.set(true);
        }
        try {
            sort.run();
        } finally {
            if (merge) {
                ACTIVE.set(false);
            }
        }
    }

    public static boolean isMergingPlayerStorage() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }

    /** After belt/disabled filtering, pull main inventory into the Inventorio sort area. */
    public static void appendPlayerStorageIfMerging(List<?> slots, List<?> out) {
        if (!isMergingPlayerStorage()) {
            return;
        }
        InventorioBeltSlots.appendPlayerMainStorageIndices(slots, out);
    }
}
