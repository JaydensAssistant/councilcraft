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
        assertEquals(25, CoinValues.replaceStock(64));
        assertEquals(50, CoinValues.replaceStock(512));
        assertEquals(100, CoinValues.replaceStock(4096));
        assertThrows(IllegalStateException.class, () -> CoinValues.replaceStock(7));
    }

    @Test
    void packValuesFollowTheEnumNames() {
        assertEquals(1, CoinValues.packValue("SPUR"));
        assertEquals(5, CoinValues.packValue("BEVEL"));
        assertEquals(10, CoinValues.packValue("SPROCKET"));
        assertEquals(25, CoinValues.packValue("COG"));
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
        // PM32c: the mold picks the coin; coins out = input Dia value / coin value (diamond 1, block 9, netherite 25).
        mint("mint_spur", 0, 1, 0, "spur", 1);
        mint("mint_spur_block", 1, 0, 0, "spur", 9);
        mint("mint_spur_netherite", 0, 0, 1, "spur", 25);
        mint("mint_bevel", 0, 5, 0, "bevel", 1);
        mint("mint_bevel_block", 1, 1, 0, "bevel", 2);
        mint("mint_bevel_netherite", 0, 0, 1, "bevel", 5);
        mint("mint_sprocket", 0, 10, 0, "sprocket", 1);
        mint("mint_sprocket_block", 1, 1, 0, "sprocket", 1);
        mint("mint_sprocket_netherite", 0, 0, 2, "sprocket", 5);
        mint("mint_cog", 0, 25, 0, "cog", 1);
        mint("mint_cog_block", 2, 7, 0, "cog", 1);
        mint("mint_cog_netherite", 0, 0, 1, "cog", 1);
        mint("mint_crown", 5, 5, 0, "crown", 1);
        mint("mint_crown_netherite", 0, 0, 2, "crown", 1);
        mint("mint_sun", 11, 1, 0, "sun", 1);
        mint("mint_sun_netherite", 0, 0, 4, "sun", 1);
    }

    @Test
    void everyMintRecipeIsValueNeutral() throws Exception {
        java.util.Map<String, Integer> coinValue = java.util.Map.of(
            "numismatics:spur", 1, "numismatics:bevel", 5, "numismatics:sprocket", 10,
            "numismatics:cog", 25, "numismatics:crown", 50, "numismatics:sun", 100);
        java.util.Map<String, Integer> inputValue = java.util.Map.of(
            "minecraft:diamond", 1, "minecraft:diamond_block", 9, "minecraft:netherite_ingot", 25);
        int files = 0;
        try (var stream = Files.list(Path.of("src/main/resources/data/councilcraft/recipes"))) {
            for (Path path : stream.filter(p -> p.getFileName().toString().startsWith("mint_")).toList()) {
                files++;
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                int in = 0;
                for (JsonElement element : json.getAsJsonArray("ingredients")) {
                    String item = element.getAsJsonObject().get("item").getAsString();
                    if (!item.endsWith("_mold")) {
                        assertTrue(inputValue.containsKey(item), path + " " + item);
                        in += inputValue.get(item);
                    }
                }
                JsonObject coins = json.getAsJsonArray("results").get(0).getAsJsonObject();
                int count = coins.has("count") ? coins.get("count").getAsInt() : 1;
                assertEquals(in, count * coinValue.get(coins.get("item").getAsString()), path.toString());
            }
        }
        assertEquals(16, files);
        assertEquals(25, dev.councilcraft.CouncilCraftRules.COG_VALUE);
    }

    private static void mint(String file, int blocks, int diamonds, int netherite, String coin, int count) throws Exception {
        expect(
            file,
            join(repeat("minecraft:diamond_block", blocks), repeat("minecraft:diamond", diamonds), repeat("minecraft:netherite_ingot", netherite), repeat("councilcraft:" + coin + "_mold", 1)),
            "numismatics:" + coin,
            count,
            "councilcraft:" + coin + "_mold"
        );
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
