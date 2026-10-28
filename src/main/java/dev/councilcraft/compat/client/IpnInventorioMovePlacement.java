package dev.councilcraft.compat.client;

/**
 * PT90. Shared placement math: on InventorioScreen, MOVE_TO_CONTAINER (↰) sits on the same
 * TopRight row as Sort, {@link #BUTTON_GAP} px to Sort's right — independent of Deep Pockets
 * background height (IPN's stock path uses setBottomRight and drifts when the GUI grows).
 */
public final class IpnInventorioMovePlacement {
    /** IPN stacks adjacent sort/move buttons 12 px apart. */
    public static final int BUTTON_GAP = 12;

    /** Vanilla IPN sort/move button size in 1.10.20. */
    public static final int BUTTON_SIZE = 10;

    /**
     * Inventorio is addChestSide and not PLAYER, so reHint bumps the shared horizontal base
     * from 7 to 19 before placing Sort. Right inset = base + horizontalOffset.
     */
    public static final int INVENTORIO_SORT_BASE_RIGHT = 19;

    private IpnInventorioMovePlacement() {}

    /** TopRight {@code right} inset for MOVE given Sort's current {@code right}. */
    public static int moveRightFromSort(int sortRight) {
        return sortRight - BUTTON_GAP;
    }

    /** Widget left from a TopRight/BottomRight {@code right} inset. */
    public static int leftFromRight(int containerWidth, int right, int buttonWidth) {
        return containerWidth - right - buttonWidth;
    }

    /** Widget top from a BottomRight {@code bottom} inset (stock IPN MOVE path). */
    public static int topFromBottom(int containerHeight, int bottom, int buttonHeight) {
        return containerHeight - bottom - buttonHeight;
    }

    /** Sort TopRight top from SORT hint {@code top} (IPN uses {@code 5 + hintTop}). */
    public static int sortTopFromHint(int hintTop) {
        return 5 + hintTop;
    }

    /** Sort TopRight right from SORT hint {@code horizontalOffset} on Inventorio. */
    public static int sortRightFromHint(int horizontalOffset) {
        return INVENTORIO_SORT_BASE_RIGHT + horizontalOffset;
    }

    /** Stock IPN MOVE BottomRight bottom from hint (non-PLAYER: {@code 85 + hintBottom}). */
    public static int moveBottomFromHint(int hintBottom) {
        return 85 + hintBottom;
    }
}
