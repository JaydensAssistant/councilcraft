package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.councilcraft.advancement.MilestoneRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** 0.4.18: PT35/PT37/PT40/PT41/PT42 wiring that unit tests can check without a world. */
class Release0418RulesTest {
    private static final Path DATA = Path.of("pack/moonlight-global-datapacks/councilcraft-data/data");
    private static final String FALSE =
        "{\"fabric:load_conditions\":[{\"condition\":\"fabric:not\",\"value\":{\"condition\":\"fabric:all_mods_loaded\",\"values\":[\"minecraft\"]}}]}";

    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    private static JsonObject json(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    private static Path adv(String id) {
        String[] parts = id.split(":", 2);
        return DATA.resolve(parts[0]).resolve("advancements").resolve(parts[1] + ".json");
    }

    private static List<Path> advancementFiles() throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.walk(DATA)) {
            s.filter(p -> p.toString().contains("/advancements/") && p.toString().endsWith(".json")).forEach(out::add);
        }
        return out;
    }

    @Test
    void pt35RemovalsUseTheAlwaysFalseCondition() throws IOException {
        for (String id : List.of("minecraft:end/root", "minecraft:story/enchant_item", "minecraft:nether/fast_travel",
            "create:train_portal", "regions_unexplored:pioneer", "betterend:root", "nullscape:root",
            "create_enchantment_industry:paradox_fusion", "minecraft:adventure/hero_of_the_village")) {
            assertEquals(JsonParser.parseString(FALSE), json(adv(id)), id);
        }
        // No S244 G7 impossible+hidden override survives (PT35 replaces them). The re-parented Supplementaries
        // globe is a jar copy whose own criterion is minecraft:impossible (the mod grants it in code).
        for (Path p : advancementFiles()) {
            if (p.endsWith("supplementaries/advancements/adventure/globe.json")) continue;
            assertFalse(Files.readString(p).contains("minecraft:impossible"), p.toString());
        }
    }

    @Test
    void voluntaryExileIsTheVisibleVanillaCopy() throws IOException {
        JsonObject d = json(adv("minecraft:adventure/voluntary_exile"));
        assertEquals("minecraft:adventure/root", d.get("parent").getAsString());
        JsonObject display = d.getAsJsonObject("display");
        assertFalse(display.get("hidden").getAsBoolean());
        assertEquals("advancements.councilcraft.voluntary_exile.description",
            display.getAsJsonObject("description").get("translate").getAsString());
        assertTrue(d.getAsJsonObject("criteria").toString().contains("minecraft:player_killed_entity"));
    }

    @Test
    void pt37TabsFoldIntoFiveRoots() throws IOException {
        JsonObject create = json(adv("create:root"));
        assertFalse(create.has("parent"));
        assertEquals("advancements.councilcraft.engineering.title",
            create.getAsJsonObject("display").getAsJsonObject("title").get("translate").getAsString());
        String[][] folds = {
            {"create_enchantment_industry:root", "create:root"}, {"numismatics:root", "create:root"},
            {"railways:root", "create:root"}, {"minecraft:niftycarts/root", "create:root"},
            {"mutantmonsters:root", "minecraft:adventure/root"}, {"betterdungeons:root", "minecraft:adventure/root"},
            {"betterdeserttemples:root", "minecraft:adventure/root"}, {"regions_unexplored:parent", "minecraft:adventure/root"},
            {"paraglider:root", "minecraft:adventure/root"}, {"farmersdelight:main/root", "minecraft:husbandry/root"},
        };
        for (String[] f : folds) {
            JsonObject d = json(adv(f[0]));
            assertEquals(f[1], d.get("parent").getAsString(), f[0]);
            assertTrue(d.has("display") && d.has("criteria"), f[0] + " is a full jar copy");
        }
    }

    @Test
    void pt42AdvancementsUseKnownMilestonesAndHaveLang() throws IOException {
        JsonObject lang = json(Path.of("src/main/resources/assets/councilcraft/lang/en_us.json"));
        Set<String> used = new HashSet<>();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(DATA.resolve("councilcraft/advancements"))) {
            s.forEach(files::add);
        }
        assertEquals(11, files.size());
        for (Path p : files) {
            JsonObject d = json(p);
            for (var e : d.getAsJsonObject("criteria").entrySet()) {
                JsonObject c = e.getValue().getAsJsonObject();
                if (c.get("trigger").getAsString().equals("councilcraft:milestone")) {
                    String m = c.getAsJsonObject("conditions").get("milestone").getAsString();
                    assertTrue(MilestoneRules.ALL.contains(m), m);
                    assertTrue(used.add(m), "milestone used twice: " + m);
                }
            }
        }
        assertEquals(new HashSet<>(MilestoneRules.ALL), used);
        // Every translate key in councilcraft-data's councilcraft-namespaced advancement text has an English string.
        Pattern key = Pattern.compile("\"translate\"\\s*:\\s*\"(advancements\\.councilcraft\\.[a-z_.]+)\"");
        for (Path p : advancementFiles()) {
            Matcher m = key.matcher(Files.readString(p));
            while (m.find()) {
                JsonElement v = lang.get(m.group(1));
                assertTrue(v != null && !v.getAsString().isBlank(), p + ": " + m.group(1));
            }
        }
        assertEquals("Testing, Testing", lang.get("advancements.councilcraft.testing_testing.title").getAsString());
    }

    @Test
    void pt42SetSailAndDoubleSaddleTags() throws IOException {
        String ships = Files.readString(DATA.resolve("councilcraft/tags/entity_types/dockyard_ships.json"));
        for (String t : List.of("cog", "brigg", "galley", "dhow", "drakkar", "galleon", "caravel")) {
            assertTrue(ships.contains("\"smallships:" + t + "\""), t);
        }
        String saddle = Files.readString(DATA.resolve("councilcraft/tags/entity_types/double_saddle.json"));
        assertTrue(saddle.contains("\"minecraft:horse\""));
        assertTrue(Files.readString(adv("councilcraft:set_sail")).contains("#councilcraft:dockyard_ships"));
        assertTrue(Files.readString(adv("councilcraft:coffee_table_book")).contains("councilcraft:printed"));
        assertTrue(Files.readString(adv("councilcraft:no_shortcuts")).contains("1000.0"));
    }

    @Test
    void milestoneRules() {
        assertFalse(MilestoneRules.pastMutantLine(2499.0, 0.0));
        assertTrue(MilestoneRules.pastMutantLine(0.0, -2500.0));
        assertTrue(MilestoneRules.pastMutantLine(1800.0, 1800.0));
        assertFalse(MilestoneRules.doubleSaddle(1));
        assertTrue(MilestoneRules.doubleSaddle(2));
        assertFalse(MilestoneRules.sharing(1));
        assertTrue(MilestoneRules.sharing(2));
        assertFalse(MilestoneRules.gotChange(new int[] {0, 0, 0}));
        assertFalse(MilestoneRules.gotChange(null));
        assertTrue(MilestoneRules.gotChange(new int[] {0, 3, 0}));
    }

    @Test
    void pt42HooksAreWired() throws IOException {
        String mod = read("src/main/resources/fabric.mod.json");
        assertTrue(mod.contains("\"voicechat\": [\n      \"dev.councilcraft.compat.CouncilCraftVoicechat\""));
        String main = read("src/main/java/dev/councilcraft/CouncilCraft.java");
        assertTrue(main.contains("Milestones.register()"));
        assertTrue(main.contains("isModLoaded(\"surveyor\")") && main.contains("SurveyorSharing.register()"));
        assertTrue(main.contains("isModLoaded(\"hardcorerevival\")") && main.contains("HardcoreRevivalRescue.register()"));
        assertTrue(read("src/main/java/dev/councilcraft/respawn/RebornFeeding.java").contains("HERE_COMES_THE_AIRPLANE"));
        assertTrue(read("src/main/java/dev/councilcraft/coin/VendorChange.java").contains("KEEP_THE_CHANGE"));
        String voice = read("src/main/java/dev/councilcraft/compat/CouncilCraftVoicechat.java");
        assertTrue(voice.contains("isInGroup()") && voice.contains("getServer().execute"));
    }

    @Test
    void pt41TipsScriptIsFifteenMinutesAndSkipsAfk() throws IOException {
        String js = read("pack/kubejs/server_scripts/councilcraft_tips.js");
        assertTrue(js.contains("const CC_TIP_INTERVAL = 20 * 60 * 15"));
        assertTrue(js.contains("const CC_TIP_ACTIVE_TAG = 'cc_active'"));
        assertTrue(js.contains("cc_tips_off") && js.contains("cc_border_width"));
        assertTrue(read("pack/kubejs/server_scripts/councilcraft_world.js").contains("const CC_BORDER_START = 10000"));
        assertTrue(js.contains("const CC_TIP_BORDER_FALLBACK = 10000"));
        // KubeJS's Rhino binds a const in a loop body only once: the shuffle must use let (0.4.18 smoke-test bug).
        String shuffle = js.substring(js.indexOf("function ccTipShuffle("), js.indexOf("function ccTipDraw("));
        String code = shuffle.lines().filter(l -> !l.trim().startsWith("//")).collect(java.util.stream.Collectors.joining("\n"));
        String loop = code.substring(code.indexOf("for (let i = bag.length - 1"));
        assertFalse(loop.contains("const "), loop);
        assertTrue(shuffle.contains("let j = ") && shuffle.contains("let t = bag[i]"));
    }

    @Test
    void pt40InventorioToggleButtonOff() throws IOException {
        assertTrue(read("pack/config/yosbr/config/inventorio.json").contains("\"ToggleButton\": false"));
    }
}
