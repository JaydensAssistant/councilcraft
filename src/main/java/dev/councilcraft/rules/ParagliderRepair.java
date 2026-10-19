package dev.councilcraft.rules;

/**
 * S244. Paragliders 20.1.4 hard-codes leather in {@code FabricParagliderItem}.
 * Phantom membrane is added. Leather still works: S244 names the new material
 * and does not say to remove the mod's own one.
 */
public final class ParagliderRepair {
    private ParagliderRepair() {
    }

    public static boolean accepts(boolean vanillaLeatherMatch, boolean isPhantomMembrane) {
        return vanillaLeatherMatch || isPhantomMembrane;
    }
}
