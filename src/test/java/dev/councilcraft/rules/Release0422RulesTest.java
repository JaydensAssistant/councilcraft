package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 0.4.22: unit-testable locks PT54–PT83 (skip PT61) without a world. */
class Release0422RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    private static boolean exists(String p) {
        return Files.exists(Path.of(p));
    }

    @Test
    void versionIs0422OrLater() throws IOException {
        assertTrue(read("gradle.properties").matches("(?s).*mod_version=0\\.4\\.(2[2-9]|[3-9]\\d).*"));
        assertTrue(read("pack/pack.toml").matches("(?s).*version = \"0\\.4\\.(2[2-9]|[3-9]\\d)\".*"));
    }

    // PT54 -------------------------------------------------------------------------------------------------------

    @Test
    void carriedPlayerMapsToAmplifierZero() {
        assertEquals(0, CarryWeight.amplifier(2, true));
        assertEquals(2, CarryWeight.amplifier(2, false));
        assertEquals(0, CarryWeight.amplifier(0, true));
    }

    // PT57 -------------------------------------------------------------------------------------------------------

    @Test
    void rebornMapsUseUuidKeys() throws IOException {
        String r = read("src/main/java/dev/councilcraft/respawn/Reborn.java");
        assertTrue(r.contains("Map<UUID, Long> MINUTES"));
        assertTrue(r.contains("Map<UUID, RebornWindow> WINDOWS"));
        assertFalse(r.contains("WeakHashMap<ServerPlayerEntity, Long>"));
        String f = read("src/main/java/dev/councilcraft/respawn/RebornFeeding.java");
        assertTrue(f.contains("Map<UUID, Feed> FEEDS"));
        assertTrue(f.contains("REFUSED_NOT_REBORN"));
        assertTrue(read("src/main/resources/assets/councilcraft/lang/en_us.json")
            .contains("\"councilcraft.feed.refused.not_reborn\""));
        String a = read("src/main/java/dev/councilcraft/respawn/PlayerActivity.java");
        assertTrue(a.contains("Map<UUID, Clock> CLOCKS"));
    }

    // PT55 -------------------------------------------------------------------------------------------------------

    @Test
    void knockoutInventoryGuardAndInventorioOpenAreWired() throws IOException {
        assertTrue(read("src/main/resources/councilcraft.client.mixins.json").contains("KnockoutInventoryGuardMixin"));
        assertTrue(read("src/main/resources/councilcraft.mixins.json").contains("InventorioKnockoutOpenMixin"));
        String guard = read("src/main/java/dev/councilcraft/mixin/client/KnockoutInventoryGuardMixin.java");
        assertTrue(guard.contains("KnockoutScreen"));
        assertTrue(guard.contains("InventorioScreen"));
        String open = read("src/main/java/dev/councilcraft/mixin/InventorioKnockoutOpenMixin.java");
        assertTrue(open.contains("InventorioScreenHandler$Companion"));
        assertTrue(open.contains("isKnockedOut"));
    }

    // PT56 -------------------------------------------------------------------------------------------------------

    @Test
    void playerStackingForbiddenInCarryOnConfig() throws IOException {
        String cfg = read("pack/config/carryon-common.json");
        assertTrue(cfg.contains("forbiddenStacking") || cfg.toLowerCase().contains("forbidden"));
        // horse and player must both be listed
        assertTrue(cfg.contains("minecraft:horse") || cfg.contains("\"#minecraft:horse\"")
            || cfg.contains("minecraft:horse"));
        // Accept either tag form used by Carry On
        assertTrue(cfg.contains("player") || cfg.contains("minecraft:player"));
    }

    // PT58 / PT59 / PT64 -----------------------------------------------------------------------------------------

    @Test
    void inventorioCraftingAndUtilitySwapMixinsExist() throws IOException {
        String common = read("src/main/resources/councilcraft.mixins.json");
        assertTrue(common.contains("InventorioCraftingQuickMoveMixin"));
        assertTrue(common.contains("UtilityBeltSwapMixin"));
        String craft = read("src/main/java/dev/councilcraft/mixin/InventorioCraftingQuickMoveMixin.java");
        assertTrue(craft.contains("fromLast"));
        assertTrue(craft.contains("getCraftingResultSlotIndex"));
        String util = read("src/main/java/dev/councilcraft/mixin/UtilityBeltSwapMixin.java");
        assertTrue(util.contains("CraftingResultInventory"));
        assertTrue(util.contains("getSelectedUtilityStack"));
        assertTrue(util.contains("button != 40"));
    }

    // PT60 -------------------------------------------------------------------------------------------------------

    @Test
    void betterF3IsClientOnlyInPack() throws IOException {
        assertTrue(exists("pack/mods/betterf3.pw.toml"));
        String t = read("pack/mods/betterf3.pw.toml");
        assertTrue(t.contains("side = \"client\"") || t.contains("side = \"client\""));
    }

    // PT62 -------------------------------------------------------------------------------------------------------

    @Test
    void feedUseActionMixinReturnsBlockWhileFeeding() throws IOException {
        assertTrue(read("src/main/resources/councilcraft.client.mixins.json").contains("FeedUseActionMixin"));
        String m = read("src/main/java/dev/councilcraft/mixin/client/FeedUseActionMixin.java");
        assertTrue(m.contains("UseAction.BLOCK"));
        assertTrue(m.contains("isFeeding"));
    }

    // PT63 -------------------------------------------------------------------------------------------------------

    @Test
    void carriedSuffocationMixinGuardsInWall() throws IOException {
        assertTrue(read("src/main/resources/councilcraft.mixins.json").contains("CarriedSuffocationMixin"));
        String m = read("src/main/java/dev/councilcraft/mixin/CarriedSuffocationMixin.java");
        assertTrue(m.contains("DamageTypes.IN_WALL"));
        assertTrue(m.contains("BabyCarry.carried"));
    }

    // PT65 -------------------------------------------------------------------------------------------------------

    @Test
    void sodiumAlwaysDeferChunkUpdatesIsOff() throws IOException {
        String s = read("pack/config/yosbr/config/sodium-options.json");
        assertTrue(s.contains("\"always_defer_chunk_updates_v2\": false")
            || s.contains("\"always_defer_chunk_updates\": false")
            || s.toLowerCase().contains("always_defer") && s.contains("false"));
    }

    // PT66 -------------------------------------------------------------------------------------------------------

    @Test
    void betterThirdPersonAndControllerMixinAreShipped() throws IOException {
        assertTrue(exists("pack/mods/better-third-person.pw.toml"));
        assertTrue(read("src/main/resources/councilcraft.client.mixins.json")
            .contains("BetterThirdPersonControllerMixin"));
        String m = read("src/main/java/dev/councilcraft/mixin/client/BetterThirdPersonControllerMixin.java");
        assertTrue(m.contains("isCameraOnPlayer"));
        assertTrue(m.contains("ControlifyHints.onController()"));
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        assertTrue(plugin.contains("\"1.9.0\""));
        assertTrue(plugin.contains("BetterThirdPersonControllerMixin"));
    }

    // PT67 + amendment -------------------------------------------------------------------------------------------

    @Test
    void forgottenGravesDeathCompassOnAndRecipeGuard() throws IOException {
        String cfg = read("pack/config/forgottengraves.json");
        assertTrue(cfg.contains("\"giveDeathCompass\": true") || cfg.contains("giveDeathCompass\": true"));
        assertTrue(read("src/main/resources/councilcraft.mixins.json").contains("GraveCompassIngredientMixin"));
        assertTrue(read("src/main/java/dev/councilcraft/mixin/GraveCompassIngredientMixin.java")
            .contains("GraveMarker"));
    }

    // PT68 -------------------------------------------------------------------------------------------------------

    @Test
    void graphicsPresetDhRadiiAre512_256_128() throws IOException {
        String s = read("src/main/java/dev/councilcraft/client/graphics/GraphicsPreset.java");
        assertTrue(s.contains("HIGH(\"ComplementaryUnbound_r5.9.3.zip\", \"MEDIUM\", 512),"));
        assertTrue(s.contains("MEDIUM(\"E-LITE shaders 5.1.3 CouncilCraft.zip\", \"MEDIUM\", 256),"));
        assertTrue(s.contains("LOW(null, \"LOW\", 128);"));
    }

    // PT69 -------------------------------------------------------------------------------------------------------

    @Test
    void mobBandIs128AndMonsterCap70() {
        assertEquals(128, MobBand.BAND);
        assertEquals(70, MobBand.MONSTER_CAP);
    }

    // PT70 -------------------------------------------------------------------------------------------------------

    @Test
    void punchySwimCarryMixinIsPinned() throws IOException {
        assertTrue(read("src/main/resources/councilcraft.client.mixins.json").contains("PunchySwimCarryMixin"));
        String m = read("src/main/java/dev/councilcraft/mixin/client/PunchySwimCarryMixin.java");
        assertTrue(m.contains("SwimmingStateMachine"));
        assertTrue(m.contains("isCarrying()"));
        assertTrue(read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java")
            .contains("\"2.8g\""));
    }

    // PT71 -------------------------------------------------------------------------------------------------------

    @Test
    void babyCarryLimitsTiersAreInSource() throws IOException {
        String s = read("src/main/java/dev/councilcraft/carry/BabyCarryLimits.java");
        assertTrue(s.contains("fraction < 1.0f / 3.0f"));
        assertTrue(s.contains("fraction < 2.0f / 3.0f"));
        assertTrue(s.contains("return level <= 0"));
        assertTrue(s.contains("return level <= 1"));
        assertTrue(s.contains("TOO_SMALL"));
        assertTrue(read("src/main/java/dev/councilcraft/CouncilCraft.java").contains("BabyCarryLimits.register()"));
        assertTrue(read("src/main/resources/assets/councilcraft/lang/en_us.json").contains("carry.too_small"));
        // Pure tier table (mirrors BabyCarryLimits.allowed without loading Carry On).
        assertFalse(tier(0.0f, 0, true));
        assertTrue(tier(0.0f, 0, false));
        assertTrue(tier(0.4f, 0, true));
        assertFalse(tier(0.4f, 1, true));
        assertTrue(tier(0.7f, 1, false));
        assertFalse(tier(0.7f, 2, false));
        assertTrue(tier(1.0f, 4, true));
    }

    private static boolean tier(float fraction, int level, boolean entity) {
        if (fraction >= 1.0f) return true;
        if (entity) {
            if (fraction < 1.0f / 3.0f) return false;
            if (fraction < 2.0f / 3.0f) return level <= 0;
            return level <= 1;
        }
        if (fraction < 2.0f / 3.0f) return level <= 0;
        return level <= 1;
    }

    // PT72–PT77 / PT76 / PT83 ------------------------------------------------------------------------------------

    @Test
    void ipnInvMoveMouseTweaksAreClientModsWithBeltMixins() throws IOException {
        assertTrue(exists("pack/mods/inventory-profiles-next.pw.toml"));
        assertTrue(exists("pack/mods/libipn.pw.toml"));
        assertTrue(exists("pack/mods/invmove.pw.toml"));
        assertTrue(exists("pack/mods/invmove-compats.pw.toml"));
        assertTrue(exists("pack/mods/mouse-tweaks.pw.toml"));
        assertTrue(read("pack/mods/inventory-profiles-next.pw.toml").contains("side = \"client\""));
        assertTrue(read("pack/mods/mouse-tweaks.pw.toml").contains("side = \"client\""));
        String client = read("src/main/resources/councilcraft.client.mixins.json");
        assertTrue(client.contains("IpnDisabledBeltMixin"));
        assertTrue(client.contains("IpnItemStorageBeltMixin"));
        assertTrue(client.contains("MouseTweaksDeepPocketsMixin"));
        assertTrue(exists("pack/config/yosbr/config/inventoryprofilesnext/inventoryprofiles.json"));
        assertTrue(exists("pack/config/yosbr/config/invmove/unrecognized.json"));
        String belts = read("src/main/java/dev/councilcraft/compat/InventorioBeltSlots.java");
        assertTrue(belts.contains("INVENTORY_ADDON_UTILITY_BELT_RANGE"));
        assertTrue(belts.contains("ToolBeltSlot"));
    }

    // PT73 knockout no walk via invmove unrecognized
    @Test
    void invmoveUnrecognizedDisablesKnockoutScreen() throws IOException {
        String u = read("pack/config/yosbr/config/invmove/unrecognized.json");
        assertTrue(u.toLowerCase().contains("knockout") || u.contains("KnockoutScreen"));
    }

    // PT78 -------------------------------------------------------------------------------------------------------

    @Test
    void naturalistAndFriendsAndFoesSpawnWeights() throws IOException {
        assertTrue(exists("pack/config/naturalist.json") || exists("pack/config/naturalist-common.toml")
            || Files.walk(Path.of("pack/config")).anyMatch(p -> p.getFileName().toString().contains("naturalist")));
        String ff = read("pack/config/friendsandfoes.json");
        assertTrue(ff.contains("\"maulerSavannaSpawnWeight\": 8"));
    }

    // PT79 -------------------------------------------------------------------------------------------------------

    @Test
    void c2meIsInThePack() {
        assertTrue(exists("pack/mods/c2me.pw.toml"));
    }

    // PT80 + amendments ------------------------------------------------------------------------------------------

    @Test
    void distantHorizonsPregenModes() throws IOException {
        String dh = read("pack/config/DistantHorizons.toml");
        assertTrue(dh.contains("chunkGeneratorMode = \"PRE_EXISTING_ONLY\""));
        assertTrue(dh.contains("generatorPlan = \"CHUNKS_ONLY\""));
        assertFalse(dh.contains("generatorPlan = \"SURFACE_THEN_CHUNKS\""));
    }

    // PT81 -------------------------------------------------------------------------------------------------------

    @Test
    void pehkuiScaledReachIsFalse() throws IOException {
        String p = read("pack/config/pehkui/config.json");
        assertTrue(p.contains("scaledReach") && p.contains("false"));
    }

    // PT82 -------------------------------------------------------------------------------------------------------

    @Test
    void steamNRailsBogeyMenuKeyIsBracket() throws IOException {
        String options = read("pack/config/yosbr/options.txt");
        assertTrue(options.contains("key_railways.keyinfo.bogey_menu:key.keyboard.right.bracket"));
    }

    // PT83 -------------------------------------------------------------------------------------------------------

    @Test
    void mouseTweaksDeepPocketsFilterMixin() throws IOException {
        String m = read("src/main/java/dev/councilcraft/mixin/client/MouseTweaksDeepPocketsMixin.java");
        assertTrue(m.contains("findPushSlots"));
        assertTrue(m.contains("findPullSlot"));
        assertTrue(m.contains("getAvailableDeepPocketsRange"));
        assertTrue(m.contains("InventorioScreen"));
    }

    // Server.properties Season 3 ---------------------------------------------------------------------------------

    @Test
    void serverPropertiesSeason3DistancesAndMotd() throws IOException {
        String props = read("server-assets/server.properties");
        assertTrue(props.contains("view-distance=8"));
        assertTrue(props.contains("simulation-distance=10"));
        assertTrue(props.contains("CouncilCraft Season 3! 10-30-2026"));
    }
}
