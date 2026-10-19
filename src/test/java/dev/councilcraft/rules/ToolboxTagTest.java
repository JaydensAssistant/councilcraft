package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Create 6.0.8.1 drops a tag file entirely when any item id in it is unknown.
 * These ids were in the draft whitelist and are not items in that build, so
 * they must stay out or a toolbox accepts nothing.
 */
class ToolboxTagTest {
    private static final String[] TAGS = {
        "/data/councilcraft/tags/items/toolbox_contents.json",
        "/data/councilcraft/tags/items/toolbox_tools.json",
        "/data/councilcraft/tags/items/toolbox_redstone.json",
        "/data/councilcraft/tags/items/toolbox_create_parts.json",
        "/data/councilcraft/tags/items/toolbox_utility.json"
    };

    /** Ids checked against Create 6.0.8.1 item models. None of them exist. */
    private static final String[] ABSENT = {
        "create:rope",
        "create:metal_girder_encased_shaft",
        "create:andesite_belt_funnel",
        "create:brass_belt_funnel",
        "create:encased_fluid_pipe",
        "create:steam_whistle_extension",
        "create:black_sail",
        "create:blue_sail",
        "create:brown_sail",
        "create:cyan_sail",
        "create:gray_sail",
        "create:green_sail",
        "create:light_blue_sail",
        "create:light_gray_sail",
        "create:lime_sail",
        "create:magenta_sail",
        "create:orange_sail",
        "create:pink_sail",
        "create:purple_sail",
        "create:red_sail",
        "create:yellow_sail",
        "create:black_nixie_tube",
        "create:blue_nixie_tube",
        "create:brown_nixie_tube",
        "create:cyan_nixie_tube",
        "create:gray_nixie_tube",
        "create:green_nixie_tube",
        "create:light_blue_nixie_tube",
        "create:light_gray_nixie_tube",
        "create:lime_nixie_tube",
        "create:magenta_nixie_tube",
        "create:orange_nixie_tube",
        "create:pink_nixie_tube",
        "create:purple_nixie_tube",
        "create:red_nixie_tube",
        "create:white_nixie_tube",
        "create:yellow_nixie_tube"
    };

    @Test
    void whitelistKeepsOnlyIdsThatExist() throws IOException {
        StringBuilder all = new StringBuilder();
        for (String tag : TAGS) {
            try (var in = ToolboxTagTest.class.getResourceAsStream(tag)) {
                assertTrue(in != null, tag);
                all.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        String text = all.toString();
        for (String id : ABSENT) {
            assertFalse(text.contains("\"" + id + "\""), id);
        }
        assertTrue(text.contains("\"create:white_sail\""));
        assertTrue(text.contains("\"create:nixie_tube\""));
        assertTrue(text.contains("\"supplementaries:rope\""));
        assertTrue(text.contains("\"create:wrench\""));
        assertFalse(text.contains("\"required\""));
    }
}
