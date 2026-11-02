package dev.councilcraft.cart;

/**
 * CC-174. Which entities phase through leaves the same way Leaf Me Alone lets
 * ridden mounts through. Leaf Me Alone has no config entity list; it only
 * clears collision for entities with a controlling passenger. Nifty carts are
 * pulled, not ridden, so they need this separate check.
 */
public final class CartLeavesRules {
    private CartLeavesRules() {
    }

    /**
     * True for Nifty Cart drawn vehicles (supply cart, animal cart, plow, …).
     * Skips the postilion helper entity.
     */
    public static boolean passesThroughLeaves(String entityTypeId) {
        if (entityTypeId == null || !entityTypeId.startsWith("niftycarts:")) {
            return false;
        }
        return !"niftycarts:postilion".equals(entityTypeId);
    }
}
