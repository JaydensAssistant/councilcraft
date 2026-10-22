package dev.councilcraft.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PPM73. Closest vanilla wood for each Regions Unexplored wood (and Vanilla Backport's pale oak), from the
 * CIELAB plank-colour distance computed by pack/tools/woodlook.py: Nifty Carts uses all 11 vanilla woods, Small Ships
 * only the 9 boat woods (no crimson / warped). The named woods are the values locked in PPM73; the 16 painted
 * planks use the computed result. kubejs/server_scripts/councilcraft_recipes.js mirrors {@link #CART}
 * (Release049RulesTest checks the two stay equal).
 */
public final class WoodLookMap {
    private WoodLookMap() {}

    /** Small Ships boat wood order (BoatEntity.Type ordinal order for the 9 vanilla boats). */
    public static final List<String> SHIP_WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak", "mangrove", "bamboo");

    public static final Map<String, String> CART = new LinkedHashMap<>();
    public static final Map<String, String> SHIP = new LinkedHashMap<>();

    private static void put(String wood, String cart, String ship) {
        CART.put(wood, cart);
        SHIP.put(wood, ship);
    }

    static {
        put("alpha", "oak", "oak");
        put("cypress", "oak", "oak");
        put("maple", "oak", "oak");
        put("pine", "oak", "oak");
        put("baobab", "cherry", "cherry");
        put("blackwood", "dark_oak", "dark_oak");
        put("brimwood", "dark_oak", "dark_oak");
        put("dead", "dark_oak", "dark_oak");
        put("eucalyptus", "jungle", "jungle");
        put("joshua", "jungle", "jungle");
        put("kapok", "jungle", "jungle");
        put("socotra", "jungle", "jungle");
        put("larch", "jungle", "jungle");
        put("magnolia", "mangrove", "mangrove");
        put("redwood", "acacia", "acacia");
        put("palm", "birch", "birch");
        put("willow", "birch", "birch");
        put("pale_oak", "birch", "birch");
        put("yellow_bioshroom", "bamboo", "bamboo");
        put("green_bioshroom", "bamboo", "bamboo");
        put("mauve", "crimson", "mangrove");
        put("pink_bioshroom", "crimson", "mangrove");
        put("blue_bioshroom", "warped", "birch");
        put("cobalt", "crimson", "dark_oak");
        // RU painted planks (computed, closest colour)
        put("black_painted", "dark_oak", "dark_oak");
        put("blue_painted", "crimson", "mangrove");
        put("brown_painted", "dark_oak", "dark_oak");
        put("cyan_painted", "warped", "spruce");
        put("gray_painted", "warped", "spruce");
        put("green_painted", "warped", "spruce");
        put("light_blue_painted", "warped", "dark_oak");
        put("light_gray_painted", "warped", "jungle");
        put("lime_painted", "bamboo", "bamboo");
        put("magenta_painted", "crimson", "mangrove");
        put("orange_painted", "acacia", "acacia");
        put("pink_painted", "crimson", "mangrove");
        put("purple_painted", "crimson", "mangrove");
        put("red_painted", "mangrove", "mangrove");
        put("white_painted", "cherry", "cherry");
        put("yellow_painted", "oak", "oak");
    }

    /** Planks item id for a mapped wood name. */
    public static String planksId(String wood) {
        return (wood.equals("pale_oak") ? "minecraft:" : "regions_unexplored:") + wood + "_planks";
    }

    /** Vanilla boat wood whose look the given planks item maps to for Small Ships, if any. */
    public static Optional<String> shipWoodForPlanks(String itemId) {
        if (itemId.startsWith("minecraft:") && itemId.endsWith("_planks")) {
            String wood = itemId.substring("minecraft:".length(), itemId.length() - "_planks".length());
            if (SHIP_WOODS.contains(wood)) return Optional.of(wood);
        }
        for (Map.Entry<String, String> e : SHIP.entrySet()) {
            if (planksId(e.getKey()).equals(itemId)) return Optional.of(e.getValue());
        }
        return Optional.empty();
    }

    /**
     * The boat wood to preselect: the mapped wood of the planks item the player holds the most of (counting only
     * planks that map to a boat wood). A tie goes to the earlier wood in {@link #SHIP_WOODS}. Empty when the player
     * holds no such planks.
     */
    public static Optional<String> preselect(Map<String, Integer> plankCounts) {
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> e : plankCounts.entrySet()) {
            Optional<String> wood = shipWoodForPlanks(e.getKey());
            int n = e.getValue();
            if (wood.isEmpty() || n <= 0) continue;
            if (n > bestCount || (n == bestCount && SHIP_WOODS.indexOf(wood.get()) < SHIP_WOODS.indexOf(best))) {
                best = wood.get();
                bestCount = n;
            }
        }
        return Optional.ofNullable(best);
    }
}
