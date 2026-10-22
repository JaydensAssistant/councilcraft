package dev.councilcraft.rules;

import java.util.Set;

/**
 * PPM67. Which mounts get "&lt;key&gt; to attach a cart" after the vanilla dismount line.
 * The same list as {@code NC_PULLERS} in pack/tools/mkconfigs.py (every Nifty Cart's pull_animals).
 */
public final class CartHint {
    public static final String NIFTY_CARTS_ACTION = "key.niftycarts.action";
    public static final String HINT = "councilcraft.mount.cart_hint";
    public static final Set<String> CART_PULLERS = Set.of(
        "minecraft:horse",
        "minecraft:donkey",
        "minecraft:mule",
        "minecraft:skeleton_horse",
        "minecraft:zombie_horse",
        "minecraft:camel",
        "naturalist:zebra"
    );

    private CartHint() {
    }

    public static boolean pullsCarts(String entityTypeId) {
        return CART_PULLERS.contains(entityTypeId);
    }
}
