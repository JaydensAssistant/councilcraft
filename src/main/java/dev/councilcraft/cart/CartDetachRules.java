package dev.councilcraft.cart;

/**
 * CC-142A / CC-B24. Pure rules for Nifty Cart hitch behavior: never auto-unhitch
 * on distance or collision; manual unhitch stays allowed. A stuck cart holds the
 * puller with a hard hitch rope (no omnidirectional speed drain).
 */
public final class CartDetachRules {
    /**
     * Extra horizontal slack beyond Nifty's relative spacing before the rope goes
     * taut. Same engagement point the old soft slowdown used ({@code spacing + 0.5}).
     */
    public static final double HITCH_SLACK = 0.5;

    private CartDetachRules() {
    }

    /** Collision-based auto-unhitch is always suppressed. */
    public static boolean allowCollisionDetach() {
        return false;
    }

    /** Distance-based auto-unhitch is always suppressed. */
    public static boolean allowDistanceDetach(double distance, double relativeSpacing) {
        return false;
    }

    /** Max horizontal distance from cart to puller while hitched. */
    public static double hitchMaxDistance(double relativeSpacing) {
        return relativeSpacing + HITCH_SLACK;
    }

    /** True when the puller is past the hitch length (rope taut). */
    public static boolean isRopeTaut(double distance, double relativeSpacing) {
        return distance > hitchMaxDistance(relativeSpacing);
    }

    /**
     * Scale to multiply the cart→puller horizontal delta by so the puller sits
     * exactly on the hitch circle. Returns 1 when the rope is slack.
     */
    public static double clampHorizontalScale(double distance, double relativeSpacing) {
        if (distance <= 1e-6) {
            return 1.0;
        }
        double max = hitchMaxDistance(relativeSpacing);
        if (distance <= max) {
            return 1.0;
        }
        return max / distance;
    }

    /**
     * Outward (away-from-cart) horizontal speed to cancel when at or past hitch
     * length. {@code awayX}/{@code awayZ} must be the unit vector from cart to
     * puller in XZ. Returns 0 when slack or when moving inward/sideways only.
     */
    public static double outwardSpeedToCancel(
        double distance,
        double relativeSpacing,
        double awayX,
        double awayZ,
        double velX,
        double velZ
    ) {
        if (distance + 1e-9 < hitchMaxDistance(relativeSpacing)) {
            return 0.0;
        }
        double outward = velX * awayX + velZ * awayZ;
        return Math.max(0.0, outward);
    }
}
