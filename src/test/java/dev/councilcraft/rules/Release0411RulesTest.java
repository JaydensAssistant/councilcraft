package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 0.4.11: PPM90-PPM99 pack fixes. */
class Release0411RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void borderGrowthStartsPausedAndOpsControlIt() throws IOException {
        String js = read("pack/kubejs/server_scripts/councilcraft_world.js");
        // PPM98: paused by default, also for worlds that already have cc_border_day
        assertTrue(js.contains("if (!d.contains('cc_border_growing')) d.putBoolean('cc_border_growing', false)"));
        assertTrue(js.contains("growing ? Math.min(CC_BORDER_CAP, w + CC_BORDER_STEP * n) : w"));
        assertTrue(js.contains("Commands.literal('ccborder').requires(src => src.hasPermission(2))"));
        for (String sub : new String[] {"reset", "pause", "resume", "status"}) {
            assertTrue(js.contains("Commands.literal('" + sub + "')"), sub);
        }
        assertTrue(js.contains("Commands.literal('endborder')"));
        assertTrue(js.contains("CC_BORDER_START = 10000"));
    }

    @Test
    void harnessesJebVillagerPagesAndCamelsAreGone() throws IOException {
        String recipes = read("pack/kubejs/server_scripts/councilcraft_recipes.js");
        assertTrue(recipes.contains("event.remove({ output: /^minecraft:[a-z_]+_harness$/ })"));
        assertTrue(recipes.contains("'transformation/minecraft/witch', 'breeding/minecraft/trader_llama', 'temper/minecraft/trader_llama'"));
        assertTrue(read("pack/kubejs/assets/emi/index/stacks/councilcraft_hidden.json").contains("\"/^minecraft:[a-z_]+_harness$/\""));
        assertFalse(Files.exists(Path.of("pack/moonlight-global-datapacks/councilcraft-data/data/councilcraft/lithostitched/worldgen_modifier/camels_rare.json")));
    }

    @Test
    void paleGardenHasNoNaturalistFauna() throws IOException {
        JsonObject m = JsonParser.parseString(read(
            "pack/moonlight-global-datapacks/councilcraft-data/data/councilcraft/lithostitched/worldgen_modifier/pale_garden_no_naturalist.json")).getAsJsonObject();
        assertEquals("lithostitched:remove_biome_spawns", m.get("type").getAsString());
        assertEquals("minecraft:pale_garden", m.get("biomes").getAsString());
        String mobs = m.get("mobs").toString();
        for (String mob : new String[] {"naturalist:bear", "naturalist:deer", "naturalist:boar", "naturalist:butterfly", "naturalist:firefly", "naturalist:snail"}) {
            assertTrue(mobs.contains(mob), mob);
        }
    }

    @Test
    void ruExpansionCarriesItsMitLicenseAndCredit() throws IOException {
        try (var zip = new java.util.zip.ZipFile("pack/moonlight-global-datapacks/ru-expansion-1.0.1-councilcraft.zip")) {
            String lic = new String(zip.getInputStream(zip.getEntry("LICENSE.txt")).readAllBytes());
            assertTrue(lic.startsWith("MIT License") && lic.contains("Copyright (c) DropoutGuy"));
        }
        assertTrue(read("pack/credits.txt").contains("RU Expansion 1.0.1 by DropoutGuy (MIT), modified for CouncilCraft"));
    }

    @Test
    void serverReadmeStatesJava21() throws IOException {
        String readme = read("pack/server-only/README.md");
        assertTrue(readme.contains("Java 21 (Temurin 21)"));
        assertFalse(readme.contains("0.2.0-mvp"));
    }

    @Test
    void jeiIsPinnedBelowGetAllIngredientsList() throws IOException {
        // PPM99: JEI 15.40+ calls IRecipeSlotView.getAllIngredientsList, which EMI 1.1.24's JEMI slots lack (client crash).
        String jei = read("pack/mods/jei.pw.toml");
        assertTrue(jei.contains("filename = \"jei-1.20.1-fabric-15.20.0.132.jar\""));
        assertTrue(jei.contains("pin = true"));
        assertTrue(jei.contains("version = \"dI7d5ZeA\""));
    }

    @Test
    void versionIs0411() throws IOException {
        assertTrue(read("gradle.properties").contains("mod_version=0.4.11\n"));
        assertTrue(read("pack/pack.toml").contains("version = \"0.4.11\""));
    }
}
