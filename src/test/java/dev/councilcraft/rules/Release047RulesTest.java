package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.respawn.RebornStats;
import dev.councilcraft.respawn.RebornText;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 0.4.7: PPM66 shared keys, PPM67 hunger line / zebra jump / Connected Paths / radial, PPM68 belt migration. */
class Release047RulesTest {
    private record Bind(String name, String key) {
    }

    @Test
    void oneKeyReachesEveryBindingOnIt() {
        List<Bind> all = List.of(new Bind("cart", "z"), new Bind("sail", "z"), new Bind("jump", "space"), new Bind("a", "unknown"), new Bind("b", "unknown"));
        assertEquals(List.of(all.get(0), all.get(1)), SharedKeys.bindingsFor(all, Bind::key, "z", "unknown"));
        assertEquals(List.of(all.get(2)), SharedKeys.bindingsFor(all, Bind::key, "space", "unknown"));
        assertTrue(SharedKeys.bindingsFor(all, Bind::key, "x", "unknown").isEmpty());
        // An unknown-key event fires nothing, not every unbound binding.
        assertTrue(SharedKeys.bindingsFor(all, Bind::key, "unknown", "unknown").isEmpty());
        assertTrue(SharedKeys.bindingsFor(all, Bind::key, null, "unknown").isEmpty());
    }

    @Test
    void toolBeltMovesPastTheRemovedSwordSlot() {
        assertEquals(0, ToolBeltMigration.remap(0));
        assertEquals(ToolBeltMigration.EXTRACT, ToolBeltMigration.remap(1));
        assertEquals(1, ToolBeltMigration.remap(2));
        assertEquals(2, ToolBeltMigration.remap(3));
        assertEquals(3, ToolBeltMigration.remap(4));
        assertEquals(-5, ToolBeltMigration.remap(-5));
        assertTrue(ToolBeltMigration.needsMigration(0));
        assertFalse(ToolBeltMigration.needsMigration(ToolBeltMigration.VERSION));
    }

    @Test
    void rebornHungerIsStatedAndEases() throws Exception {
        assertEquals(150, RebornText.startingHungerPercent());
        assertEquals(150, RebornText.hungerPercent(RebornStats.at(0L)));
        assertEquals(100, RebornText.hungerPercent(RebornStats.at(CouncilCraftRules.REBORN_ACTIVE_MINUTES)));
        int halfway = RebornText.hungerPercent(RebornStats.at(CouncilCraftRules.REBORN_ACTIVE_MINUTES / 2L));
        assertTrue(halfway > 100 && halfway < 150, "halfway hunger " + halfway);
        String lang = Files.readString(Path.of("src/main/resources/assets/councilcraft/lang/en_us.json"));
        assertTrue(lang.contains("\"effect.councilcraft.reborn.now\": \"Now: %s%% size, %s%% speed, %s%% strength, %s%% HP, %s%% hunger\""));
        // PPM97: the long hunger line and its lang key are gone; the Now line's "150% hunger" is the only hunger text.
        assertFalse(lang.contains("\"effect.councilcraft.reborn.hunger\""));
        assertTrue(lang.contains("\"" + RebornText.SPURT_DESC + "\""));
        assertTrue(lang.contains("\"councilcraft.feed.refused.spurt\""));
        assertTrue(lang.contains("\"councilcraft.mount.cart_hint\": \"%s to attach a cart\""));
    }

    @Test
    void zebraJumpMapsOntoTheHorseBand() {
        assertEquals(0.4, HorseJump.zebraEquivalentStrength(0.4), 1e-9);
        assertEquals(0.7, HorseJump.zebraEquivalentStrength(0.45), 1e-9);
        assertEquals(1.0, HorseJump.zebraEquivalentStrength(0.5), 1e-9);
        assertEquals(0.4, HorseJump.zebraEquivalentStrength(0.1), 1e-9);
        assertEquals(1.0, HorseJump.zebraEquivalentStrength(0.9), 1e-9);
        // The average zebra now jumps like the average horse.
        assertEquals(HorseJump.strength(0.7, 0f), HorseJump.strength(HorseJump.zebraEquivalentStrength(0.45), 0f), 1e-9);
    }

    @Test
    void connectedPathsDropsStrippedBlocksAndLimitsSnow() {
        Set<String> stripped = Set.of("minecraft:oak_log", "minecraft:obsidian", "regions_unexplored:cobalt_obsidian");
        String stone = "matchBlocks=minecraft:stone minecraft:oak_log minecraft:obsidian minecraft:andesite\nmethod=overlay\nlayer=cutout\ntiles=0-16\nconnectTiles=minecraft:stone\n";
        assertEquals("matchBlocks=minecraft:stone minecraft:andesite\nmethod=overlay\nlayer=cutout\ntiles=0-16\nconnectTiles=minecraft:stone\n",
            ConnectedPathsRules.rewrite(stone, stripped, "minecraft:stone", "stone"));
        String onlyStripped = "matchBlocks=minecraft:obsidian regions_unexplored:cobalt_obsidian\nmethod=overlay\n";
        assertEquals("matchBlocks=minecraft:gravel\nmethod=overlay\n",
            ConnectedPathsRules.rewrite(onlyStripped, stripped, "minecraft:gravel", "gravel"));
        String snow = "matchBlocks=minecraft:snow_block minecraft:dirt\nmethod=overlay\ntiles=0-16\n";
        assertEquals("matchBlocks=minecraft:snow_block minecraft:dirt\nmethod=overlay\ntiles=0-16\nfaces=top bottom\n",
            ConnectedPathsRules.rewrite(snow, stripped, "minecraft:snow_block", "grass_block_snow"));
        assertEquals("matchBlocks=a\nfaces=top bottom\n",
            ConnectedPathsRules.rewrite("matchBlocks=a\nfaces=all\n", Set.of(), "a", "powder_snow"));
        assertTrue(ConnectedPathsRules.STRIPPED_GROUPS.containsAll(List.of("logs", "stripped_logs", "woods", "stripped_woods", "obsidians")));
        assertFalse(ConnectedPathsRules.stripped("planks"));
        assertEquals(List.of("rocks", "planks"), ConnectedPathsRules.withoutStripped(List.of("rocks", "logs", "planks", "obsidians")));
        assertTrue(ConnectedPathsRules.markerCurrent(ConnectedPathsRules.VERSION + "\n"));
        assertFalse(ConnectedPathsRules.markerCurrent(null));
        assertFalse(ConnectedPathsRules.markerCurrent("councilcraft-0.4.6"));
    }

    @Test
    void controllerRadialExtrasUseControlifyModdedIds() {
        assertEquals("key.niftycarts.action", ControllerRadialDefaults.bindingPath("key.niftycarts.action"));
        assertEquals("supplementaries.keybind.quiver", ControllerRadialDefaults.bindingPath("supplementaries.keybind.quiver"));
        assertEquals("key.some_mod.do_thing_", ControllerRadialDefaults.bindingPath("key.Some Mod.do thing!"));
        // PPM105 (0.4.13): slot 7 is now the Next quiver arrow action; values are Controlify binding ids.
        // PT7 amendment (0.4.14): slot 5 (pick block) is now Simple Voice Chat's menu key; drop stack (slot 2) stays.
        assertEquals(Map.of(0, "fabric-key-binding-api-v1:key.niftycarts.action", 5, "fabric-key-binding-api-v1:key.voice_chat",
            6, "fabric-key-binding-api-v1:key.smallships.ship_sail", 7, "councilcraft:next_quiver_arrow"), ControllerRadialDefaults.EXTRAS);
        assertEquals("fabric-key-binding-api-v1:key.voice_chat", ControllerRadialDefaults.extraFor(5));
        assertEquals(null, ControllerRadialDefaults.extraFor(2));
    }

    @Test
    void cartHintMatchesTheNiftyCartsPullers() throws Exception {
        String mkconfigs = Files.readString(Path.of("pack/tools/mkconfigs.py"));
        for (String puller : CartHint.CART_PULLERS) {
            assertTrue(mkconfigs.contains("'" + puller + "'"), puller);
        }
        assertTrue(CartHint.pullsCarts("naturalist:zebra"));
        assertFalse(CartHint.pullsCarts("minecraft:pig"));
    }
}
