package dev.councilcraft.compat.client;

import dev.councilcraft.compat.InventorioBeltSlots;
import java.util.List;
import net.minecraft.screen.ScreenHandler;

/**
 * CC-B23A / CC-B23AA (replaces CC-B23 merge). On Inventorio's screen, one Sort click runs
 * IPN's sort twice: Deep Pockets slots only, then main inventory slots 9–35 only. Hotbar stays
 * untouched. Items never cross between the two areas.
 *
 * <p>When Deep Pockets are locked/absent, pass 1 leaves {@code sortableItemStorage} empty so IPN
 * falls back to player storage (CC-92); pass 2 still sorts main 9–35 in place. Chests and other
 * screens are unchanged (single {@code sort.run()} with no ThreadLocal pass).
 *
 * <p>Move Matching still uses {@code itemStorage} / {@code fillSlots} without a sort pass, so Deep
 * Pockets stay the container target (PT90).
 */
public final class IpnInventorioSortMerge {
    /** Which Inventorio Sort pass is active on this thread. */
    public enum Pass {
        OFF,
        /** Keep fillSlots result after belt filter = unlocked Deep Pockets only. */
        DEEP_POCKETS,
        /** Replace fillSlots result with PlayerInventory indices 9–35 only. */
        MAIN
    }

    private static final ThreadLocal<Pass> PASS = ThreadLocal.withInitial(() -> Pass.OFF);

    private IpnInventorioSortMerge() {
    }

    /**
     * Inventorio: Deep Pockets pass then main pass. Other handlers: one plain sort.
     */
    public static void runSeparatePlayerSorts(ScreenHandler handler, Runnable sort) {
        if (!InventorioBeltSlots.isInventorioScreenHandler(handler)) {
            sort.run();
            return;
        }
        PASS.set(Pass.DEEP_POCKETS);
        try {
            sort.run();
        } finally {
            PASS.set(Pass.OFF);
        }
        PASS.set(Pass.MAIN);
        try {
            sort.run();
        } finally {
            PASS.set(Pass.OFF);
        }
    }

    public static Pass currentPass() {
        return PASS.get();
    }

    /**
     * After belt/disabled filtering on {@code fillSlots}: for MAIN, clear container indices and
     * keep only player main storage; for DEEP_POCKETS, leave pockets-only list alone.
     */
    public static void applySortPassSlotFilter(List<?> slots, List<?> out) {
        Pass pass = PASS.get();
        if (pass == Pass.OFF || out == null) {
            return;
        }
        if (pass == Pass.MAIN) {
            out.clear();
            InventorioBeltSlots.appendPlayerMainStorageIndices(slots, out);
        }
        // DEEP_POCKETS: no append — unlocked Deep Pockets already in out; locked rows filtered out.
    }

    /** Test-only: run body with a fixed pass (does not nest inventorio detection). */
    public static void runWithPassForTest(Pass pass, Runnable body) {
        Pass prev = PASS.get();
        PASS.set(pass);
        try {
            body.run();
        } finally {
            PASS.set(prev);
        }
    }

    /**
     * Test-only: same two-pass sequence as Inventorio without needing an InventorioScreenHandler.
     */
    public static void runBothPassesForTest(Runnable sort) {
        PASS.set(Pass.DEEP_POCKETS);
        try {
            sort.run();
        } finally {
            PASS.set(Pass.OFF);
        }
        PASS.set(Pass.MAIN);
        try {
            sort.run();
        } finally {
            PASS.set(Pass.OFF);
        }
    }
}
