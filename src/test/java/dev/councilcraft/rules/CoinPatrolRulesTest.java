package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Coin numbers, the patrol roll, and the minting recipe files. No world. */
class CoinPatrolRulesTest {
    @Test
    void packValuesReplaceTheNumismaticsStockNumbers() {
        assertEquals(1, CoinValues.replaceStock(1));
        assertEquals(5, CoinValues.replaceStock(8));
        assertEquals(10, CoinValues.replaceStock(16));
        assertEquals(20, CoinValues.replaceStock(64));
        assertEquals(50, CoinValues.replaceStock(512));
        assertEquals(100, CoinValues.replaceStock(4096));
        assertThrows(IllegalStateException.class, () -> CoinValues.replaceStock(7));
    }

    @Test
    void packValuesFollowTheEnumNames() {
        assertEquals(1, CoinValues.packValue("SPUR"));
        assertEquals(5, CoinValues.packValue("BEVEL"));
        assertEquals(10, CoinValues.packValue("SPROCKET"));
        assertEquals(20, CoinValues.packValue("COG"));
        assertEquals(50, CoinValues.packValue("CROWN"));
        assertEquals(100, CoinValues.packValue("SUN"));
        assertNull(CoinValues.packValue("GOLD"));
        assertNull(CoinValues.packValue(null));
    }

    @Test
    void onePatrolRollInFourJoinsAnEvoker() {
        assertEquals(4, PatrolEvoker.sides());
        assertTrue(PatrolEvoker.joins(0));
        assertFalse(PatrolEvoker.joins(1));
        assertFalse(PatrolEvoker.joins(2));
        assertFalse(PatrolEvoker.joins(3));
        int joined = 0;
        for (int roll = 0; roll < PatrolEvoker.sides(); roll++) {
            if (PatrolEvoker.joins(roll)) {
                joined++;
            }
        }
        assertEquals(1, joined);
    }

    @Test
    void sprocketAndCogBeatADiamondBlockWhileThoseMoldsArePresent() {
        assertTrue(MintPriority.competesWithDiamondBlock("councilcraft:sprocket_mold"));
        assertTrue(MintPriority.competesWithDiamondBlock("councilcraft:cog_mold"));
        assertFalse(MintPriority.competesWithDiamondBlock("councilcraft:spur_mold"));
        assertFalse(MintPriority.competesWithDiamondBlock("councilcraft:bevel_mold"));
        assertFalse(MintPriority.competesWithDiamondBlock("councilcraft:crown_mold"));
        assertFalse(MintPriority.competesWithDiamondBlock("councilcraft:sun_mold"));
        assertFalse(MintPriority.competesWithDiamondBlock("minecraft:diamond"));

        assertTrue(MintPriority.competingMold(java.util.List.of("minecraft:diamond", "councilcraft:sprocket_mold")));
        assertTrue(MintPriority.competingMold(java.util.List.of("councilcraft:cog_mold", "minecraft:diamond")));
        assertFalse(MintPriority.competingMold(java.util.List.of("minecraft:diamond", "councilcraft:spur_mold")));

        assertTrue(MintPriority.suppressDiamondBlock(true, "minecraft:diamond_block"));
        assertFalse(MintPriority.suppressDiamondBlock(false, "minecraft:diamond_block"));
        assertFalse(MintPriority.suppressDiamondBlock(true, "numismatics:sprocket"));
        assertFalse(MintPriority.suppressDiamondBlock(true, "numismatics:cog"));
        assertFalse(MintPriority.suppressDiamondBlock(true, null));
    }

    @Test
    void mintingRecipesReturnTheMoldAndSpendTheLockedInputs() throws Exception {
        expect("mint_spur", join(repeat("minecraft:diamond", 1), repeat("councilcraft:spur_mold", 1)), "numismatics:spur", 1, "councilcraft:spur_mold");
        expect("mint_bevel", join(repeat("minecraft:diamond", 5), repeat("councilcraft:bevel_mold", 1)), "numismatics:bevel", 5, "councilcraft:bevel_mold");
        expect("mint_sprocket", join(repeat("minecraft:diamond", 10), repeat("councilcraft:sprocket_mold", 1)), "numismatics:sprocket", 10, "councilcraft:sprocket_mold");
        expect("mint_cog", join(repeat("minecraft:diamond", 20), repeat("councilcraft:cog_mold", 1)), "numismatics:cog", 20, "councilcraft:cog_mold");
        expect(
            "mint_crown",
            join(repeat("minecraft:diamond_block", 5), repeat("minecraft:diamond", 5), repeat("councilcraft:crown_mold", 1)),
            "numismatics:crown",
            50,
            "councilcraft:crown_mold"
        );
        expect(
            "mint_sun",
            join(repeat("minecraft:diamond_block", 11), repeat("minecraft:diamond", 1), repeat("councilcraft:sun_mold", 1)),
            "numismatics:sun",
            100,
            "councilcraft:sun_mold"
        );
        expect("mint_cog_netherite", join(repeat("minecraft:netherite_ingot", 1), repeat("councilcraft:cog_mold", 1)), "numismatics:cog", 20, "councilcraft:cog_mold");
        expect("mint_sun_netherite", join(repeat("minecraft:netherite_ingot", 5), repeat("councilcraft:sun_mold", 1)), "numismatics:sun", 100, "councilcraft:sun_mold");
    }

    private static List<String> repeat(String item, int count) {
        return Collections.nCopies(count, item);
    }

    @SafeVarargs
    private static List<String> join(List<String>... parts) {
        List<String> all = new ArrayList<>();
        for (List<String> part : parts) {
            all.addAll(part);
        }
        return all;
    }

    private static void expect(String file, List<String> ingredients, String coin, int coinCount, String mold) throws Exception {
        Path path = Path.of("src/main/resources/data/councilcraft/recipes", file + ".json");
        JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals("create:compacting", json.get("type").getAsString(), file);
        List<String> found = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("ingredients")) {
            String item = element.getAsJsonObject().get("item").getAsString();
            assertFalse(item.contains("emerald"), file);
            found.add(item);
        }
        assertEquals(ingredients, found, file);
        assertEquals(2, json.getAsJsonArray("results").size(), file);
        JsonObject coins = json.getAsJsonArray("results").get(0).getAsJsonObject();
        JsonObject returned = json.getAsJsonArray("results").get(1).getAsJsonObject();
        assertEquals(coin, coins.get("item").getAsString(), file);
        int count = coins.has("count") ? coins.get("count").getAsInt() : 1;
        assertEquals(coinCount, count, file);
        assertEquals(mold, returned.get("item").getAsString(), file);
        assertFalse(returned.has("count") && returned.get("count").getAsInt() != 1, file);
    }
}
