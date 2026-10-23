package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 0.4.10: PPM84-PPM89 shipped configs and registrations. */
class Release0410RulesTest {
    private static JsonObject json(String path) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject();
    }

    private static Map<String, String> options() throws IOException {
        Map<String, String> keys = new HashMap<>();
        for (String line : Files.readAllLines(Path.of("pack/config/yosbr/options.txt"))) {
            if (line.startsWith("key_")) {
                int colon = line.lastIndexOf(':');
                assertEquals(null, keys.put(line.substring(4, colon), line.substring(colon + 1)), "duplicate " + line);
            }
        }
        return keys;
    }

    private static boolean listed(String config, String list, String mixin) throws IOException {
        JsonArray mixins = json("src/main/resources/" + config).getAsJsonArray(list);
        for (int i = 0; i < mixins.size(); i++) {
            if (mixins.get(i).getAsString().equals(mixin)) return true;
        }
        return false;
    }

    // ---- PPM84 / PPM86 / PPM87 registrations ----

    @Test
    void swapHandsFixIsCommonAndControllerFixesAreClientOnly() throws IOException {
        assertTrue(listed("councilcraft.mixins.json", "mixins", "InventorioSwapHandsMixin"));
        assertFalse(listed("councilcraft.client.mixins.json", "client", "InventorioSwapHandsMixin"));
        for (String m : List.of("ControllerInventorioScreenMixin", "ControllerKeyboardShaderMixin")) {
            assertTrue(listed("councilcraft.client.mixins.json", "client", m), m);
            assertFalse(listed("councilcraft.mixins.json", "mixins", m), m);
        }
    }

    // ---- PPM85 ----

    @Test
    void radialOnDpadUpAndChatOffTheDpad() throws IOException {
        JsonObject d = json("pack/kubejs/assets/controlify/controllers/default_bind/default.json").getAsJsonObject("defaults");
        assertEquals("controlify:button/dpad_up", d.getAsJsonObject("controlify:radial_menu").get("button").getAsString());
        assertEquals("empty", d.getAsJsonObject("controlify:open_chat").get("type").getAsString());
        // Utility belt unchanged on D-pad Left / Right, pick block still off the D-pad.
        assertEquals("controlify:button/dpad_left",
            d.getAsJsonObject("fabric-key-binding-api-v1:inventorio.keys.prev_utility").get("button").getAsString());
        assertEquals("controlify:button/dpad_right",
            d.getAsJsonObject("fabric-key-binding-api-v1:inventorio.keys.next_utility").get("button").getAsString());
        assertEquals("empty", d.getAsJsonObject("controlify:pick_block").get("type").getAsString());
        // Nothing in the pack binds Circle (east) any more.
        assertFalse(d.toString().contains("button/east"));
        // The radial's chat slot (Controlify slot 3) is not replaced by a pack extra.
        assertEquals(null, ControllerRadialDefaults.extraFor(3));
    }

    @Test
    void controlifyDoesNotKeepDefaultBindingsInSaves() throws IOException {
        // Only non-default bindings are saved, so the new defaults reach existing configs.
        assertFalse(json("pack/config/yosbr/config/controlify.json").toString().contains("keepDefaultBindings\":true"));
    }

    // ---- PPM88 ----

    @Test
    void utilityScrollLabelAndHints() throws IOException {
        JsonObject lang = json("src/main/resources/assets/councilcraft/lang/en_us.json");
        assertEquals("Hold to scroll utility belt", lang.get("key.councilcraft.utility_scroll").getAsString());
        assertEquals("Hold %s + Scroll to switch belt tools", lang.get("councilcraft.hint.utility_belt.keyboard").getAsString());
        assertTrue(lang.get("councilcraft.hint.utility_belt.controller").getAsString().contains("%s / %s"));
        assertTrue(lang.has("councilcraft.hint.utility_belt.title"));
    }

    // ---- PPM89 ----

    @Test
    void defaultKeybindCleanup() throws IOException {
        Map<String, String> k = options();
        String none = "key.keyboard.unknown";
        assertEquals(none, k.get("inventorio.keys.open_player_settings"));
        assertEquals("key.keyboard.i", k.get("key.punchy.inspect"));
        assertEquals(none, k.get("key.punchy.open_main_menu"));
        for (String t : List.of("sword_inspect_old", "sword_inspect_rework", "punchy_sword_inspect_old", "punchy_sword_inspect_rework")) {
            assertEquals(none, k.get("key.punchy.triggerable." + t), t);
        }
        assertEquals(none, k.get("inventorio.keys.next_utility"));
        assertEquals(none, k.get("inventorio.keys.prev_utility"));
        assertEquals("key.keyboard.k", k.get("iris.keybind.toggleShaders"));
        assertEquals(none, k.get("key.fabricskyboxes.toggle.debug_hud"));
        // Unchanged from 0.4.9.
        assertEquals("key.keyboard.h", k.get("inventorio.keys.empty_utility"));
        // Nothing else is on I.
        assertEquals(1, k.values().stream().filter("key.keyboard.i"::equals).count());
    }

    @Test
    void punchyTriggerableKeysShipUnbound() throws IOException {
        // Punchy restores triggerable keys from config/punchy/triggerable-keybindings.json, not options.txt.
        JsonObject t = json("pack/config/yosbr/config/punchy/triggerable-keybindings.json");
        assertEquals(4, t.size());
        for (String id : List.of("sword_inspect_old", "sword_inspect_rework", "punchy_sword_inspect_old", "punchy_sword_inspect_rework")) {
            assertEquals("key.keyboard.unknown", t.get(id).getAsString(), id);
        }
    }

    @Test
    void versionIs0410() throws IOException {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.10\n"));
        assertTrue(Files.readString(Path.of("pack/pack.toml")).contains("version = \"0.4.10\""));
    }
}
