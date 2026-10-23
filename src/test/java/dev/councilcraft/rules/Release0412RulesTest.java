package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** 0.4.12: PPM100-PPM104. */
class Release0412RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void surveyorExplorationFollowsImmersivePortalsChunkSending() throws IOException {
        // PPM100: no mixin; the IP signal feeds Surveyor, only when both mods are loaded.
        String main = read("src/main/java/dev/councilcraft/CouncilCraft.java");
        assertTrue(main.contains("isModLoaded(\"imm_ptl_core\") && FabricLoader.getInstance().isModLoaded(\"surveyor\")"));
        assertTrue(main.contains("dev.councilcraft.compat.SurveyorPortalExploration.register();"));
        String hook = read("src/main/java/dev/councilcraft/compat/SurveyorPortalExploration.java");
        assertTrue(hook.contains("NewChunkTrackingGraph.beginWatchChunkSignal.connect((player, pos) ->"));
        assertTrue(hook.contains("SurveyorExploration.of(player).addChunk(pos.dimension, pos.getChunkPos(), false)"));
        assertTrue(hook.contains("if (connected) {"));
        // The Surveyor / IP types are compile-time stubs, never shipped.
        for (String stub : new String[] {
            "folk/sisby/surveyor/SurveyorExploration.java",
            "qouteall/imm_ptl/core/chunk_loading/NewChunkTrackingGraph.java",
            "qouteall/imm_ptl/core/chunk_loading/DimensionalChunkPos.java",
            "qouteall/q_misc_util/my_util/SignalBiArged.java",
            "punchy/config/PunchyConfig.java"}) {
            assertTrue(Files.exists(Path.of("src/stubs/java/" + stub)), stub);
            assertFalse(Files.exists(Path.of("src/main/java/" + stub)), stub);
        }
        assertFalse(read("build.gradle").contains("maven.modrinth:surveyor"));
    }

    @Test
    void vanillaLookTogglesPunchy() throws IOException {
        // PPM101: the Vanilla look turns Punchy off (and back on), with a one-time startup sync; Punchy pinned.
        String applier = read("src/main/java/dev/councilcraft/client/graphics/VanillaLookApplier.java");
        int sync = applier.indexOf("        syncPunchy(on);");
        assertTrue(sync > applier.indexOf("manager.setEnabledProfiles(wanted);"));
        assertTrue(sync < applier.indexOf("client.options.refreshResourcePacks(manager);"));
        assertTrue(applier.contains("if (FabricLoader.getInstance().isModLoaded(\"punchy\")) {"));
        String compat = read("src/main/java/dev/councilcraft/client/compat/PunchyVanillaLook.java");
        assertTrue(compat.contains("boolean wanted = !vanillaLookOn;"));
        assertTrue(compat.contains("PunchyConfig.applyAndSave(data);"));
        assertTrue(compat.contains("catch (LinkageError | RuntimeException e)"));
        String client = read("src/main/java/dev/councilcraft/CouncilCraftClient.java");
        assertTrue(client.contains("ClientLifecycleEvents.CLIENT_STARTED.register(client -> {"));
        assertTrue(client.contains("VanillaLookApplier.syncPunchy(true);"));
        String punchy = read("pack/mods/punchy-fpa.pw.toml");
        assertTrue(punchy.contains("pin = true"));
        assertTrue(punchy.contains("filename = \"punchy-2.8g-fabric-1.20.1.jar\""));
    }

    @Test
    void tierColorsMatchTheSpec() {
        assertEquals(List.of(2, 5, 10, 15, 20), SupporterRules.TIERS);
        assertEquals("#55FF55", SupporterRules.hex(SupporterRules.tierColor(2)));
        assertEquals("#55FFFF", SupporterRules.hex(SupporterRules.tierColor(5)));
        assertEquals("#5599FF", SupporterRules.hex(SupporterRules.tierColor(10)));
        assertEquals("#FF77AA", SupporterRules.hex(SupporterRules.tierColor(15)));
        assertEquals("#FFAA00", SupporterRules.hex(SupporterRules.tierColor(20)));
        assertFalse(SupporterRules.isTier(3));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.tierColor(1));
        assertEquals("\u2605", SupporterRules.BADGE);
    }

    @Test
    void nameColorIsRoleThenGoldForTwentyThenDefault() {
        assertEquals(OptionalInt.of(0x9B30FF), SupporterRules.nameColor(0x9B30FF, 20));
        assertEquals(OptionalInt.of(0x9B30FF), SupporterRules.nameColor(0x9B30FF, null));
        assertEquals(OptionalInt.of(0xFFAA00), SupporterRules.nameColor(null, 20));
        assertEquals(OptionalInt.empty(), SupporterRules.nameColor(null, 15));
        assertEquals(OptionalInt.empty(), SupporterRules.nameColor(null, null));
    }

    @Test
    void roleSpecsAndIds() {
        SupporterRules.RoleSpec spec = SupporterRules.parseRoleSpec("#9b30ff \"[President]\"");
        assertEquals(0x9B30FF, spec.color());
        assertEquals("[President]", spec.prefix());
        assertEquals("", SupporterRules.parseRoleSpec("#00AA00").prefix());
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("red \"[X]\""));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("#12345 \"[X]\""));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("#123456 \"\u00A7c[X]\""));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("#123456 \"" + "x".repeat(33) + "\""));
        assertTrue(SupporterRules.isValidRoleId("vice_president"));
        assertFalse(SupporterRules.isValidRoleId("President"));
        assertFalse(SupporterRules.isValidRoleId(""));
        assertEquals(OptionalInt.of(0xFFAA00), SupporterRules.parseHexColor("#ffaa00"));
        assertEquals(OptionalInt.empty(), SupporterRules.parseHexColor("ffaa00"));
    }

    @Test
    void footerListsTiersHighToLowAndHidesEmptyOnes() {
        Map<String, Integer> donors = new LinkedHashMap<>();
        donors.put("Maple_Fox", 20);
        donors.put("juniper", 20);
        donors.put("Aldric", 20);
        donors.put("Bram", 2);
        donors.put("Cass", 10);
        Map<Integer, List<String>> lines = SupporterRules.footerLines(donors);
        assertEquals(List.of(20, 10, 2), List.copyOf(lines.keySet()));
        assertEquals(List.of("Aldric", "juniper", "Maple_Fox"), lines.get(20));
        assertTrue(SupporterRules.footerLines(Map.of()).isEmpty());
    }

    @Test
    void supporterCommandsDisplayAndSaving() throws IOException {
        // PPM102: op-only commands, badge then prefix then name in chat (display name) and the tab list, world save.
        String s = read("src/main/java/dev/councilcraft/supporter/Supporters.java");
        assertTrue(s.contains("CommandManager.literal(\"ccdonor\")"));
        assertTrue(s.contains("CommandManager.literal(\"ccrole\")"));
        assertTrue(s.contains("hasPermissionLevel(2)"));
        for (String sub : new String[] {"add", "remove", "list", "create", "assign", "delete"}) {
            assertTrue(s.contains("CommandManager.literal(\"" + sub + "\")"), sub);
        }
        assertTrue(s.contains("PlayerListHeaderS2CPacket"));
        assertTrue(s.contains("PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME"));
        String state = read("src/main/java/dev/councilcraft/supporter/SupporterState.java");
        assertTrue(state.contains("extends PersistentState"));
        assertTrue(state.contains("\"councilcraft_supporters\""));
        String mixins = read("src/main/resources/councilcraft.mixins.json");
        assertTrue(mixins.contains("\"SupporterDisplayNameMixin\","));
        assertTrue(mixins.contains("\"SupporterListNameMixin\","));
        assertTrue(read("src/main/java/dev/councilcraft/CouncilCraft.java").contains("dev.councilcraft.supporter.Supporters.register();"));
    }

    @Test
    void pvpHeadsDropBesideTheGraveIncludingHardcoreRevivalKnockouts() throws IOException {
        // PPM103 / PPM104.
        String js = read("pack/kubejs/server_scripts/councilcraft_heads.js");
        assertTrue(js.contains("if (String(attacker.getUUID()) === String(victim.getUUID())) return null"));
        assertTrue(js.contains("return direct instanceof CcProjectile ? attacker : null"));
        assertTrue(js.contains("'Slain by ' + killerName"));
        assertTrue(js.contains("victim.drop(head, true, false)"));
        assertTrue(js.contains("Platform.isLoaded('hardcorerevival')"));
        assertTrue(js.contains("HardcoreRevivalManager').NOT_RESCUED_IN_TIME"));
        assertTrue(js.contains("String(hrAttacker) === rec.getString('uuid')"));
        assertTrue(js.contains("if (!ccKnockedOut(p)) p.persistentData.remove(CC_KO_KEY)"));
        assertTrue(js.contains("CcNbtUtils.writeGameProfile(new CcCompoundTag(), victim.getGameProfile())"));
    }
}
