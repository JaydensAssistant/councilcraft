package dev.councilcraft.cart;

/**
 * CC-142A. Pure rules for Nifty Cart hitch behavior: never auto-unhitch on
 * distance or collision; manual unhitch stays allowed. Stuck/lagging cart
 * holds the pulling entity back.
 */
public final class CartDetachRules {
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

    /**
     * True when the cart is lagging far enough behind that the puller should
     * be slowed (holds the horse back). {@code lag} is distance minus spacing.
     */
    public static boolean shouldHoldPullerBack(double distance, double relativeSpacing) {
        return distance > relativeSpacing + 0.5;
    }

    /**
     * Movement-speed multiplier applied to the puller while the cart lags.
     * 1.0 = no change; lower values hold the horse back harder.
     */
    public static float pullerSlowFactor(double distance, double relativeSpacing) {
        if (!shouldHoldPullerBack(distance, relativeSpacing)) {
            return 1f;
        }
        double lag = distance - relativeSpacing;
        // Soft clamp: more lag → slower, floor at 0.2 so the horse never freezes solid.
        float factor = (float) (1.0 / (1.0 + lag));
        return Math.max(0.2f, Math.min(1f, factor));
    }
}
