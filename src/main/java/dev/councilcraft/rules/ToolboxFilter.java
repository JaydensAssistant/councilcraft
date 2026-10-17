package dev.councilcraft.rules;

/**
 * S234 / S235. A toolbox slot accepts an item only when the item is in
 * {@code councilcraft:toolbox_contents}. An empty insert is allowed so a
 * slot can still be cleared. There is no slowdown and no slot cap.
 */
public final class ToolboxFilter {
    private ToolboxFilter() {
    }

    public static boolean allows(boolean emptyInsert, boolean inWhitelist) {
        return emptyInsert || inWhitelist;
    }
}
