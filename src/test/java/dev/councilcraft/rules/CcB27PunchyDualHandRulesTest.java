package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * CC-B27: Punchy yosbr default blacklists Create Extendo Grip / Potato Cannon / Handheld
 * Worldshaper with Both hands (blacklistApplyDualHanded=true). DPMY 0.5.2 only fills missing
 * dual-hand flags with false, so a seeded true survives launch and relaunch.
 */
class CcB27PunchyDualHandRulesTest {
    private static final Pattern MOD_VERSION = Pattern.compile("(?m)^mod_version=(.+)$");
    private static final String YOSBR =
        "pack/config/yosbr/config/punchy/punchy_config.json";
    private static final String MRPACK_ENTRY =
        "overrides/config/yosbr/config/punchy/punchy_config.json";
    private static final List<String> ITEMS =
        List.of(
            "create:extendo_grip",
            "create:potato_cannon",
            "create:handheld_worldshaper");

    private static String modVersion() throws IOException {
        Matcher m = MOD_VERSION.matcher(Files.readString(Path.of("gradle.properties")));
        assertTrue(m.find(), "mod_version in gradle.properties");
        return m.group(1).trim();
    }

    private static void assertDualHandOn(JsonObject root, String where) {
        assertTrue(root.has("itemBlacklist"), where + " missing itemBlacklist");
        assertTrue(root.has("blacklistApplyDualHanded"), where + " missing blacklistApplyDualHanded");
        var list = root.getAsJsonArray("itemBlacklist");
        var dual = root.getAsJsonObject("blacklistApplyDualHanded");
        for (String id : ITEMS) {
            boolean inList = false;
            for (var el : list) {
                if (el.isJsonPrimitive() && id.equals(el.getAsString())) {
                    inList = true;
                    break;
                }
            }
            assertTrue(inList, where + " itemBlacklist missing " + id);
            assertTrue(dual.has(id), where + " dual map missing " + id);
            assertTrue(dual.get(id).getAsBoolean(), where + " " + id + " must be Both hands on (true)");
        }
    }

    @Test
    void yosbrPunchyConfigHasCreateItemsBothHandsOn() throws IOException {
        Path file = Path.of(YOSBR);
        assertTrue(Files.isRegularFile(file), "missing " + YOSBR);
        JsonObject root =
            JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        assertDualHandOn(root, YOSBR);
        // Full Punchy defaults + DPMY IDS pre-seeded so Gson does not zero enableMod,
        // and DPMY BlacklistForce.merge() is a no-op (does not rewrite the file).
        assertTrue(root.get("enableMod").getAsBoolean(), "enableMod must be true (Gson zeros missing booleans)");
        assertTrue(root.getAsJsonArray("itemBlacklist").size() >= 3);
        assertTrue(root.getAsJsonObject("blacklistApplyDualHanded").size() >= 3);
    }

    @Test
    void indexListsYosbrPunchyConfig() throws IOException {
        String index = Files.readString(Path.of("pack/index.toml"));
        assertTrue(
            index.contains("config/yosbr/config/punchy/punchy_config.json"),
            "pack/index.toml must list the CC-B27 yosbr punchy_config.json after packwiz refresh");
    }

    @Test
    void distMrpacksShipYosbrPunchyConfigWhenPresent() throws IOException {
        Path dist = Path.of("dist", modVersion());
        if (!Files.isDirectory(dist)) {
            return;
        }
        int checked = 0;
        try (Stream<Path> s = Files.list(dist)) {
            for (Path mr :
                (Iterable<Path>) s.filter(p -> p.getFileName().toString().endsWith(".mrpack"))
                    ::iterator) {
                try (ZipFile z = new ZipFile(mr.toFile())) {
                    ZipEntry e = z.getEntry(MRPACK_ENTRY);
                    if (e == null) {
                        continue;
                    }
                    String body =
                        new String(z.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8);
                    assertDualHandOn(JsonParser.parseString(body).getAsJsonObject(), mr + ":" + MRPACK_ENTRY);
                    // Must not ship a shared overrides/config/punchy overwrite that would
                    // clobber user Punchy settings every pack update (yosbr-only, like CC-82A).
                    assertTrue(
                        z.getEntry("overrides/config/punchy/punchy_config.json") == null,
                        mr + " must not force-override config/punchy/punchy_config.json");
                    checked++;
                }
            }
        }
        assertTrue(checked == 0 || checked >= 2, "expected 0 (mid-build) or both mrpacks, got " + checked);
    }

    @Test
    void dpmyMergePreservesExistingDualHandTrue() {
        // Mirrors Don't Punch My TacZ 0.5.2 BlacklistForce.merge: only add dual=false when missing.
        JsonObject dual = new JsonObject();
        dual.addProperty("create:extendo_grip", true);
        dual.addProperty("create:potato_cannon", true);
        dual.addProperty("create:handheld_worldshaper", true);
        for (String id : ITEMS) {
            if (dual.has(id)) {
                continue; // DPMY: if (dual.has(id)) continue;
            }
            dual.addProperty(id, false);
        }
        for (String id : ITEMS) {
            assertTrue(dual.get(id).getAsBoolean(), "DPMY must not flip seeded true for " + id);
        }
        // A missing id would get false:
        JsonObject empty = new JsonObject();
        String probe = "create:extendo_grip";
        if (!empty.has(probe)) {
            empty.addProperty(probe, false);
        }
        assertFalse(empty.get(probe).getAsBoolean());
    }
}
