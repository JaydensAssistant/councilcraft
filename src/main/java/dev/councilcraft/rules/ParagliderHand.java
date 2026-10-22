package dev.councilcraft.rules;

/**
 * PPM70. Which hand Paragliders 20.1.4 reads the paraglider from. The mod reads only the main hand.
 * The main hand still wins when it holds a paraglider. Otherwise the offhand is used, so a tool or
 * block can stay in the main hand.
 *
 * <p>"Offhand" means the stack the game puts in the off hand ({@code PlayerEntity.getOffHandStack}).
 * With Inventorio 1.10.4 that is the selected utility-belt slot: Inventorio returns it from
 * {@code getEquippedStack(OFFHAND)}, renders it in the off hand and stores offhand equips in it.
 * Inventorio never uses the vanilla offhand list. A paraglider in any other belt slot is not in the
 * off hand and does not count. The belt is never searched.
 */
public final class ParagliderHand {
    public enum Hand { MAIN, OFF, NONE }

    private ParagliderHand() {
    }

    public static Hand pick(boolean mainIsParaglider, boolean offIsParaglider) {
        if (mainIsParaglider) {
            return Hand.MAIN;
        }
        return offIsParaglider ? Hand.OFF : Hand.NONE;
    }

    /** The Paragliding flag (open-glider model, arm pose) goes on the one stack in use, and only while gliding. */
    public static boolean flagged(boolean isStackInUse, boolean paragliding) {
        return isStackInUse && paragliding;
    }
}
