package dev.councilcraft.rules;

/**
 * PPM68. Inventorio stores the tool belt by position. Its stock belt is pickaxe, sword, axe, shovel,
 * hoe (slots 0 to 4); CouncilCraft removes the sword slot, so the belt is pickaxe, axe, shovel, hoe.
 * A save from before has to move: 0 stays 0, 1 (the sword) leaves the belt, 2 to 4 move down one.
 * Pure numbers, testable without a world.
 */
public final class ToolBeltMigration {
    /** Int tag in the player's "Inventorio" compound once the belt is in the 4-slot layout. */
    public static final String MARKER = "CouncilCraftToolBelt";
    public static final int VERSION = 1;
    /** The removed sword slot's old position. */
    public static final int OLD_SWORD_SLOT = 1;
    /** {@link #remap} result for the old sword slot: the stack goes to the main inventory. */
    public static final int EXTRACT = -1;

    private ToolBeltMigration() {
    }

    /**
     * New position for an item at {@code oldSlot}: 0 to 0, 1 to {@link #EXTRACT}, n &ge; 2 to n - 1.
     * A negative (already invalid) slot is kept, so Inventorio handles it as it always has.
     */
    public static int remap(int oldSlot) {
        if (oldSlot < OLD_SWORD_SLOT) {
            return oldSlot;
        }
        if (oldSlot == OLD_SWORD_SLOT) {
            return EXTRACT;
        }
        return oldSlot - 1;
    }

    /** Whether a saved Inventorio compound with this marker value still needs the move. */
    public static boolean needsMigration(int savedMarker) {
        return savedMarker < VERSION;
    }
}
