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

/** 0.4.14: PT1-PT4, PT6-PT9, CC-5 (1:1 Nether portals), PvP heads DamageSource fix. */
class Release0414RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void waveyCapesIsAClientOnlyModrinthLink() throws IOException {
        // PT1.
        String t = read("pack/mods/wavey-capes.pw.toml");
        assertTrue(t.contains("side = \"client\""));
        assertTrue(t.contains("mod-id = \"kYuIpRLv\""));
        assertTrue(t.contains("version = \"hlPN4g5v\""));
        assertTrue(t.contains("url = \"https://cdn.modrinth.com/data/kYuIpRLv/versions/hlPN4g5v/"));
    }

    @Test
    void sparkLedgerAndDiscordIntegrationAreServerSideInTheMainPack() throws IOException {
        // PT6 / PT8: pack/server-only/ never reaches a Modrinth import.
        for (String[] m : new String[][] {{"spark", "l6YH9Als", "XGW2fviP"}, {"ledger", "LVN9ygNV", "85vYzkP3"},
                {"dcintegration", "rbJ7eS5V", "WLOprDDs"}}) {
            String t = read("pack/mods/" + m[0] + ".pw.toml");
            assertTrue(t.contains("side = \"server\""), m[0]);
            assertTrue(t.contains("mod-id = \"" + m[1] + "\""), m[0]);
            assertTrue(t.contains("version = \"" + m[2] + "\""), m[0]);
            assertFalse(Files.exists(Path.of("pack/server-only/" + m[0] + ".pw.toml")), m[0]);
        }
        // The bot token lives only on the server: no Discord Integration config is shipped.
        assertFalse(Files.exists(Path.of("pack/config/Discord-Integration.toml")));
        assertFalse(Files.exists(Path.of("pack/config/yosbr/config/Discord-Integration.toml")));
        assertTrue(read("pack/.packwizignore").contains("server-only/"));
        String readme = read("pack/server-only/README.md");
        assertTrue(readme.contains("Java 21 (Temurin 21)"));
        assertTrue(readme.contains("does NOT ship config/Discord-Integration.toml"));
    }

    @Test
    void immersivePortalsConfigIsADirectOverride() throws IOException {
        // PT2: the server reads it too (yosbr is client-only).
        JsonObject ip = JsonParser.parseString(read("pack/config/immersive_portals.json")).getAsJsonObject();
        assertFalse(ip.get("enableWarning").getAsBoolean());
        assertFalse(ip.get("checkModInfoFromInternet").getAsBoolean());
        assertFalse(ip.get("enableUpdateNotification").getAsBoolean());
        // CC-5 keeps IP's own see-through nether portals.
        assertEquals("normal", ip.get("netherPortalMode").getAsString());
        assertFalse(Files.exists(Path.of("pack/config/yosbr/config/immersive_portals.json")));
        assertTrue(read("pack/tools/mkconfigs.py").contains("js('immersive_portals.json', _ip)"));
    }

    @Test
    void moobloomsSpawnInRegionsUnexploredFlowerBiomes() throws IOException {
        // PT3: tag addition only.
        JsonObject tag = JsonParser.parseString(read(
            "pack/moonlight-global-datapacks/councilcraft-data/data/friendsandfoes/tags/worldgen/biome/has_moobloom/buttercup.json"))
            .getAsJsonObject();
        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals("[\"regions_unexplored:flower_fields\",\"regions_unexplored:magnolia_woodland\"]", tag.get("values").toString());
    }

    @Test
    void freshlyAnimatedLegacySitsBetweenFreshAnimationsAndFreshlyAnimated() throws IOException {
        // PT4 amendment (bottom-to-top).
        String options = read("pack/config/yosbr/options.txt");
        assertTrue(options.contains("\"file/FreshAnimations_v1.11.1_1.20+.zip\",\"vanillabackport:freshly_animated_legacy\",\"vanillabackport:freshly_animated\""));
        String look = read("src/main/java/dev/councilcraft/rules/VanillaLook.java");
        assertTrue(look.contains("\"vanillabackport:freshly_animated_legacy\""));
    }

    @Test
    void voiceChatMenuIsOnVAndTheRadialSlotFive() throws IOException {
        // PT7 + amendment.
        String options = read("pack/config/yosbr/options.txt");
        assertTrue(options.contains("\nkey_key.voice_chat:key.keyboard.v\n"));
        assertFalse(options.contains("key_key.voice_chat:key.keyboard.right.alt"));
        assertEquals(1, options.split("key\\.keyboard\\.v\n", -1).length - 1, "V is bound once");
        assertEquals("fabric-key-binding-api-v1:key.voice_chat", ControllerRadialDefaults.extraFor(5));
        JsonObject lang = JsonParser.parseString(read("src/main/resources/assets/councilcraft/lang/en_us.json")).getAsJsonObject();
        boolean guide = false;
        for (String k : lang.keySet()) {
            String v = lang.get(k).getAsString();
            assertFalse(v.contains("Right Alt"), k);
            if (k.startsWith("councilcraft.guide.line") && v.contains("Voice chat: V")) guide = true;
        }
        assertTrue(guide);
    }

    @Test
    void voiceChatServerConfigIsNotShipped() throws IOException {
        // PT9: the host's port survives pack updates.
        assertFalse(Files.exists(Path.of("pack/config/voicechat/voicechat-server.properties")));
        assertFalse(read("pack/tools/mkconfigs.py").contains("voicechat-server.properties', "));
    }

    @Test
    void netherPortalsAreOneToOne() throws IOException {
        // CC-5: IP 5.2.0's intrinsic nether generation hardcodes 8:1.
        String mixin = read("src/main/java/dev/councilcraft/mixin/NetherPortalRatioMixin.java");
        assertTrue(mixin.contains("targets = \"qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration\""));
        assertTrue(mixin.contains("method = \"<clinit>\""));
        assertTrue(mixin.contains("ordinal = 0"));
        assertTrue(mixin.contains("index = 2"));
        assertTrue(mixin.contains("require = 1"));
        assertTrue(mixin.contains("return 1;"));
        assertTrue(read("src/main/resources/councilcraft.mixins.json").contains("\"NetherPortalRatioMixin\""));
    }

    @Test
    void pvpHeadsUseKubeJsDamageSourceNames() throws IOException {
        // KubeJS 6 renames DamageSource#getEntity/getDirectEntity to getActual/getImmediate for scripts.
        String js = read("pack/kubejs/server_scripts/councilcraft_heads.js");
        assertFalse(js.contains(".getEntity()"));
        assertFalse(js.contains(".getDirectEntity()"));
        assertTrue(js.contains("const attacker = source.getActual()"));
        assertTrue(js.contains("const direct = source.getImmediate()"));
        assertTrue(js.contains("const attacker = event.source.getActual()"));
    }
}
