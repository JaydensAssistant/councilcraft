package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** 0.4.9: PPM72-PPM83 rules and shipped configs. */
class Release049RulesTest {
    private static final Set<String> CART_WOODS = Set.of("oak", "spruce", "birch", "jungle", "acacia", "cherry",
        "dark_oak", "mangrove", "bamboo", "crimson", "warped");

    // ---- PPM73 / PPM74 / PPM75 wood look ----

    @Test
    void lockedWoodLooks() {
        assertEquals("cherry", WoodLookMap.CART.get("baobab"));
        assertEquals("birch", WoodLookMap.CART.get("pale_oak"));
        assertEquals("crimson", WoodLookMap.CART.get("mauve"));
        assertEquals("mangrove", WoodLookMap.SHIP.get("mauve"));
        assertEquals("warped", WoodLookMap.CART.get("blue_bioshroom"));
        assertEquals("birch", WoodLookMap.SHIP.get("blue_bioshroom"));
        assertEquals("dark_oak", WoodLookMap.SHIP.get("cobalt"));
        assertEquals(40, WoodLookMap.CART.size());
    }

    @Test
    void shipsNeverUseNetherWoods() {
        for (String wood : WoodLookMap.SHIP.values()) assertTrue(WoodLookMap.SHIP_WOODS.contains(wood), wood);
        for (String wood : WoodLookMap.CART.values()) assertTrue(CART_WOODS.contains(wood), wood);
        assertEquals(WoodLookMap.CART.keySet(), WoodLookMap.SHIP.keySet());
    }

    @Test
    void kubejsWoodLookMirrorsTheJavaCartMap() throws IOException {
        String js = Files.readString(Path.of("pack/kubejs/server_scripts/councilcraft_recipes.js"));
        int start = js.indexOf("const WOOD_LOOK = {");
        String body = js.substring(start, js.indexOf('}', start));
        Map<String, String> parsed = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(\\w+): '(\\w+)'").matcher(body);
        while (m.find()) parsed.put(m.group(1), m.group(2));
        assertEquals(WoodLookMap.CART, parsed);
    }

    @Test
    void preselectPicksTheMostHeldPlanks() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("regions_unexplored:baobab_planks", 10);
        counts.put("minecraft:spruce_planks", 12);
        counts.put("minecraft:stone", 64);
        assertEquals(Optional.of("spruce"), WoodLookMap.preselect(counts));
        counts.put("regions_unexplored:mauve_planks", 30);
        assertEquals(Optional.of("mangrove"), WoodLookMap.preselect(counts));
    }

    @Test
    void preselectTieGoesToTheEarlierBoatWood() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("minecraft:birch_planks", 8);
        counts.put("regions_unexplored:alpha_planks", 8); // maps to oak
        assertEquals(Optional.of("oak"), WoodLookMap.preselect(counts));
    }

    @Test
    void preselectIgnoresNetherAndUnmappedPlanks() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("minecraft:crimson_planks", 64);
        counts.put("minecraft:warped_planks", 64);
        counts.put("othermod:fir_planks", 64);
        counts.put("minecraft:oak_planks", 0);
        assertEquals(Optional.empty(), WoodLookMap.preselect(counts));
        assertEquals(Optional.of("birch"), WoodLookMap.shipWoodForPlanks("minecraft:pale_oak_planks"));
    }

    // ---- PPM78 Vanilla look ----

    private static List<String> shippedPackOrder() throws IOException {
        String line = Files.readAllLines(Path.of("pack/config/yosbr/options.txt")).stream()
            .filter(l -> l.startsWith("resourcePacks:")).findFirst().orElseThrow();
        return JsonParser.parseString(line.substring("resourcePacks:".length())).getAsJsonArray()
            .asList().stream().map(e -> e.getAsString()).toList();
    }

    @Test
    void vanillaLookKnowsTheShippedOrder() throws IOException {
        assertEquals(shippedPackOrder(), VanillaLook.DEFAULT_ORDER);
        assertTrue(VanillaLook.DEFAULT_ORDER.containsAll(VanillaLook.OFF));
        assertTrue(VanillaLook.DEFAULT_ORDER.contains("file/vanillatweaks-councilcraft.zip"));
        assertFalse(VanillaLook.OFF.contains("file/vanillatweaks-councilcraft.zip"));
    }

    @Test
    void vanillaLookOnRemovesOnlyItsPacks() {
        List<String> on = VanillaLook.apply(VanillaLook.DEFAULT_ORDER, true, p -> true);
        assertEquals(List.of("fabric", "continuity:default", "file/councilcraft-printed-copies.zip",
            "file/vanillatweaks-councilcraft.zip", "file/Better-Leaves-9.6.zip", "file/Antique_Atlas_4_Compat_v1.4.zip",
            "file/councilcraft-atlas-ru.zip"), on);
        assertTrue(VanillaLook.isOn(on, p -> true));
        assertFalse(VanillaLook.isOn(VanillaLook.DEFAULT_ORDER, p -> true));
    }

    @Test
    void vanillaLookOffRestoresTheShippedOrder() {
        List<String> on = VanillaLook.apply(VanillaLook.DEFAULT_ORDER, true, p -> true);
        assertEquals(VanillaLook.DEFAULT_ORDER, VanillaLook.apply(on, false, p -> true));
    }

    @Test
    void vanillaLookOffKeepsThePlayersOwnPacksAndSkipsMissingOnes() {
        List<String> mine = new ArrayList<>(List.of("fabric", "file/MyPack.zip", "file/vanillatweaks-councilcraft.zip"));
        List<String> off = VanillaLook.apply(mine, false, p -> !p.equals("punchy:punchy"));
        assertEquals(List.of("fabric", "file/Bibliophilia 1.17 (1.20.1).zip", "file/councilcraft-book-priority.zip",
            "file/MyPack.zip", "file/vanillatweaks-councilcraft.zip", "file/Modded Swords x Punchy! 1.20.1 - 1.21.1 v1.5.zip",
            "file/Drigo-3D-Lanterns-x-Punchy.zip", "minecraft:supporteatinganimation", "file/FreshAnimations_v1.11.1_1.20+.zip",
            "vanillabackport:freshly_animated_legacy", "vanillabackport:freshly_animated", "file/5.0 FA Illager Mod Compats.zip",
            "file/-1.21.2 Fresh Moves v3.1 (With Animated Eyes).zip", "file/Connected-Paths.zip", "file/Connected-Bricks.zip",
            "file/Connected-Rocks.zip", "file/councilcraft-connected-glass.zip", "file/councilcraft-bettergrass-ru.zip",
            "lambdabettergrass:default"), off);
    }

    @Test
    void vanillaLookIsOffWhenNoPackCanBeSwitched() {
        assertFalse(VanillaLook.isOn(List.of(), p -> false));
    }

    // ---- PPM81 monster band ----

    @Test
    void monsterBandIsOneHundredTwentyEightBlocksClampedToTheWorld() {
        assertEquals(128, MobBand.BAND);
        assertEquals(70, MobBand.MONSTER_CAP);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {-64, 84}, MobBand.range(-44, -64, 90));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {-58, 71}, MobBand.range(70, -64, 71));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {-64, 140}, MobBand.range(34, -64, 140));
    }

    @Test
    void monsterBandIsEmptyHighAboveTheSurface() {
        assertNull(MobBand.range(300, -64, 71));
        assertTrue(MobBand.inBand(10, 74));
        assertFalse(MobBand.inBand(-55, 74));
    }

    // ---- PPM72 Every Compat, PPM77 book priority, PPM79 guide, PPM82/83 Immersive Portals ----

    @Test
    void everyCompHazardousConfigIsForcedAndBlocksBetterEndAndRegionsUnexplored() throws IOException {
        Path forced = Path.of("pack/config/everycomp-hazardous.json");
        assertFalse(Files.exists(Path.of("pack/config/yosbr/config/everycomp-hazardous.json")));
        JsonObject cfg = JsonParser.parseString(Files.readString(forced)).getAsJsonObject();
        assertEquals("betterend:.*", cfg.getAsJsonObject("woodtype").getAsJsonArray("blacklist").get(0).getAsString());
        assertEquals("regions_unexplored", cfg.getAsJsonObject("module").getAsJsonArray("blacklist").get(0).getAsString());
        String meta = Files.readString(Path.of("pack/mods/every-compat.pw.toml"));
        assertTrue(meta.contains("side = \"both\""));
    }

    @Test
    void bookPriorityPackOutweighsNothingAboveTen() throws IOException {
        try (var zip = new java.util.zip.ZipFile("pack/resourcepacks/councilcraft-book-priority.zip")) {
            List<String> rules = zip.stream().map(e -> e.getName()).filter(n -> n.endsWith(".properties")).toList();
            assertEquals(45, rules.size());
            for (String rule : rules) {
                String text = new String(zip.getInputStream(zip.getEntry(rule)).readAllBytes());
                assertTrue(text.contains("weight=5\n") || text.endsWith("weight=5"), rule);
            }
        }
    }

    @Test
    void modSettingsGuideLinesExist() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(
            Path.of("src/main/resources/assets/councilcraft/lang/en_us.json"))).getAsJsonObject();
        for (int i = 1; i <= 7; i++) assertTrue(lang.has("councilcraft.guide.line" + i), "line" + i);
        for (String key : List.of("councilcraft.guide.title", "councilcraft.guide.button",
            "councilcraft.graphics.vanilla_look", "councilcraft.graphics.vanilla_look.hint")) assertTrue(lang.has(key), key);
    }

    @Test
    void immersivePortalsConfigShipsAsOneDirectOverrideWithNoAlternateDimensions() throws IOException {
        // 0.4.14 (PT2): one direct override for both sides; the yosbr and server-only copies are gone.
        assertFalse(Files.exists(Path.of("pack/config/yosbr/config/immersive_portals.json")));
        assertFalse(Files.exists(Path.of("pack/server-only/config/immersive_portals.json")));
        JsonObject ip = JsonParser.parseString(Files.readString(
            Path.of("pack/config/immersive_portals.json"))).getAsJsonObject();
        assertEquals(2, ip.get("portalRenderLimit").getAsInt());
        assertEquals(2, ip.get("maxPortalLayer").getAsInt());
        assertFalse(ip.get("enableAlternateDimensions").getAsBoolean());
    }
}
