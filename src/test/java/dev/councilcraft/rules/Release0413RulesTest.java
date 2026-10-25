package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** 0.4.13: PPM105-PPM108. */
class Release0413RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void nextQuiverArrowSendsSupplementariesCycleMessage() throws IOException {
        // PPM105: an isolated compat class sends Supplementaries' own cycle message, guarded and logged.
        String cycle = read("src/main/java/dev/councilcraft/client/compat/SupplementariesQuiverCycle.java");
        assertTrue(cycle.contains("quiverPlayer.supplementaries$getQuiverSlot()"));
        assertTrue(cycle.contains("ModNetwork.CHANNEL.sendToServer(new ServerBoundCycleSelectableContainerItemPacket(1, slot));"));
        assertTrue(cycle.contains("item.getData(stack).cycle(1);"));
        String action = read("src/main/java/dev/councilcraft/client/compat/ControlifyQuiverCycle.java");
        assertTrue(action.contains("implements ControlifyEntrypoint"));
        assertTrue(action.contains("loader.isModLoaded(\"controlify\") && loader.isModLoaded(\"supplementaries\")"));
        assertTrue(action.contains("if (!shouldRegister()) {"));
        assertTrue(action.contains(".radialCandidate(id));"));
        assertTrue(action.contains("input.justPressed()"));
        assertEquals(2, action.split("catch \\(LinkageError \\| RuntimeException e\\)", -1).length - 1);
        // Supplementaries types stay out of the entrypoint class, so it always loads.
        assertFalse(action.contains("net.mehvahdjukaar"));
        String mod = read("src/main/resources/fabric.mod.json");
        assertTrue(mod.contains("\"controlify\": [\n      \"dev.councilcraft.client.compat.ControlifyQuiverCycle\"\n    ]"));
        String lang = read("src/main/resources/assets/councilcraft/lang/en_us.json");
        assertTrue(lang.contains("\"councilcraft.controller.next_quiver_arrow\": \"Next quiver arrow\""));
        // Moonlight (Supplementaries' network library) is compiled against, never bundled.
        String gradle = read("build.gradle");
        assertTrue(gradle.contains("modImplementation \"maven.modrinth:moonlight:1.20-2.16.37-fabric\""));
        assertFalse(gradle.contains("include \"maven.modrinth:moonlight"));
        assertTrue(read("pack/mods/moonlight.pw.toml").contains("moonlight-1.20-2.16.37-fabric.jar"));
        assertTrue(gradle.contains("modImplementation \"maven.modrinth:supplementaries:1.20-3.1.44-fabric\""));
    }

    @Test
    void radialSlotSevenIsTheNextQuiverArrowAction() throws IOException {
        assertEquals("councilcraft:next_quiver_arrow", ControllerRadialDefaults.NEXT_QUIVER_ARROW);
        assertEquals(ControllerRadialDefaults.NEXT_QUIVER_ARROW, ControllerRadialDefaults.extraFor(7));
        assertEquals("fabric-key-binding-api-v1:key.niftycarts.action", ControllerRadialDefaults.extraFor(0));
        assertFalse(ControllerRadialDefaults.EXTRAS.containsValue(ControllerRadialDefaults.moddedKey("supplementaries.keybind.quiver")));
        String mixin = read("src/main/java/dev/councilcraft/mixin/client/ControllerRadialDefaultsMixin.java");
        assertTrue(mixin.contains("ControlifyBindApi.get().getAllBindIds().anyMatch(id::equals)"));
    }

    @Test
    void pennyMeltsAndEveryCoinNamesItsYield() throws IOException {
        // PPM106.
        String recipes = read("pack/kubejs/server_scripts/councilcraft_recipes.js");
        assertTrue(recipes.contains("event.custom({ type: 'create:crushing', ingredients: [{ item: 'numismatics:spur' }], results: [{ item: 'minecraft:diamond', count: 1, chance: 0.8 }], processingTime: 250 }).id('councilcraft:melt_spur')"));
        assertTrue(recipes.contains(";[['bevel', 4], ['sprocket', 8], ['cog', 20], ['crown', 40], ['sun', 80]]"));
        assertFalse(recipes.contains("no 1-coin recipe"));
        String tips = read("pack/kubejs/client_scripts/councilcraft_tooltips.js");
        for (String line : new String[] {"['spur', '1 diamond (80% chance)']", "['bevel', '4 diamonds']", "['sprocket', '8 diamonds']",
            "['cog', '20 diamonds']", "['crown', '40 diamonds']", "['sun', '80 diamonds']"}) {
            assertTrue(tips.contains(line), line);
        }
        assertTrue(tips.contains("event.add('numismatics:' + c[0], Text.gold('Melts to: ' + c[1]))"));
    }

    @Test
    void badgeLadderColorsAndBoldStars() {
        // PPM107.
        assertEquals(List.of(2, 5, 10, 15, 20), SupporterRules.TIERS);
        assertEquals("#D9814C", SupporterRules.hex(SupporterRules.tierColor(2)));
        assertEquals("#D8D8D8", SupporterRules.hex(SupporterRules.tierColor(5)));
        assertEquals("#3DDC6E", SupporterRules.hex(SupporterRules.tierColor(10)));
        assertEquals("#4DE8E0", SupporterRules.hex(SupporterRules.tierColor(15)));
        assertEquals("#FFAA00", SupporterRules.hex(SupporterRules.tierColor(20)));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.tierColor(1));
        assertFalse(SupporterRules.isTier(3));
        assertEquals("\u2605", SupporterRules.BADGE);
        assertFalse(SupporterRules.boldBadge(2));
        assertFalse(SupporterRules.boldBadge(5));
        assertFalse(SupporterRules.boldBadge(10));
        assertTrue(SupporterRules.boldBadge(15));
        assertTrue(SupporterRules.boldBadge(20));
    }

    @Test
    void nameColorIsRoleThenGoldForTwentyThenDefaultBlue() {
        // PPM107: role > $20 gold > default blue.
        assertEquals(0x9B30FF, SupporterRules.nameColor(0x9B30FF, 20));
        assertEquals(0x9B30FF, SupporterRules.nameColor(0x9B30FF, 2));
        assertEquals(0x9B30FF, SupporterRules.nameColor(0x9B30FF, null));
        assertEquals(0xFFAA00, SupporterRules.nameColor(null, 20));
        assertEquals(0x7FB2FF, SupporterRules.nameColor(null, 15));
        assertEquals(0x7FB2FF, SupporterRules.nameColor(null, 2));
        assertEquals(0x7FB2FF, SupporterRules.nameColor(null, null));
        assertEquals(0x7FB2FF, SupporterRules.DEFAULT_NAME);
    }

    @Test
    void everyNameIsDecoratedBadgePrefixNameWithBoldStarsOnly() throws IOException {
        String s = read("src/main/java/dev/councilcraft/supporter/Supporters.java");
        int badge = s.indexOf("out.append(badge(donor.tier(), SupporterRules.BADGE))");
        int prefix = s.indexOf("out.append(colored(role.prefix() + \" \", role.color()));");
        int name = s.indexOf("return out.append(name.copy().styled(style -> style.withColor(TextColor.fromRgb(color))));");
        assertTrue(badge > 0 && badge < prefix && prefix < name);
        assertFalse(s.contains("if (donor == null && role == null) {"));
        // Bold only through badge(): the star and the footer label, never the name.
        assertEquals(1, s.split("withBold\\(", -1).length - 1);
        assertTrue(s.contains("style.withBold(SupporterRules.boldBadge(tier))"));
        assertTrue(s.contains(".append(badge(tier, SupporterRules.BADGE + \" $\" + tier))"));
        assertTrue(s.contains(".append(Text.literal(\"  \" + String.join(\", \", names)).formatted(Formatting.GRAY)));"));
        assertTrue(Files.exists(Path.of("docs/mockups/flashy-names-A.png")));
    }

    @Test
    void roleCreateTakesANameAColorAndAnOptionalPrefix() {
        // PPM108.
        SupporterRules.RoleSpec president = SupporterRules.parseRoleSpec("President", "royal_purple");
        assertEquals("president", president.id());
        assertEquals(0x9B30FF, president.color());
        assertEquals("[President]", president.prefix());
        SupporterRules.RoleSpec vp = SupporterRules.parseRoleSpec("Vice_President", "GOLD \"[VP]\"");
        assertEquals("vice_president", vp.id());
        assertEquals(0xFFAA00, vp.color());
        assertEquals("[VP]", vp.prefix());
        assertEquals("", SupporterRules.parseRoleSpec("Mayor", "#00aa00 \"\"").prefix());
        // The 0.4.12 form still works: lowercase id, #hex, quoted prefix.
        SupporterRules.RoleSpec old = SupporterRules.parseRoleSpec("president", "#9b30ff \"[President]\"");
        assertEquals("president", old.id());
        assertEquals(0x9B30FF, old.color());
        assertEquals("[President]", old.prefix());
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("Mayor", "pink"));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("Mayor", "#12345"));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("Mayor", ""));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("Mayor", "red \"\u00A7c[X]\""));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("Mayor", "red \"" + "x".repeat(33) + "\""));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("Vice-President", "red"));
        assertThrows(IllegalArgumentException.class, () -> SupporterRules.parseRoleSpec("x".repeat(33), "red"));
    }

    @Test
    void colorNamesAndCaseInsensitiveIds() {
        assertEquals(List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray",
            "blue", "green", "aqua", "red", "light_purple", "yellow", "white", "purple", "royal_purple"),
            List.copyOf(SupporterRules.NAMED_COLORS.keySet()));
        assertEquals(OptionalInt.of(0xFF5555), SupporterRules.parseColor("Red"));
        assertEquals(OptionalInt.of(0x9B30FF), SupporterRules.parseColor("purple"));
        assertEquals(OptionalInt.of(0x123ABC), SupporterRules.parseColor("#123abc"));
        assertEquals(OptionalInt.empty(), SupporterRules.parseColor("123abc"));
        assertEquals("president", SupporterRules.roleId("PRESIDENT"));
        assertTrue(SupporterRules.isValidRoleId("vice_president"));
        assertFalse(SupporterRules.isValidRoleId("President"));
        assertFalse(SupporterRules.isValidRoleId(""));
    }

    @Test
    void ccroleCommandsUseNameColorSuggestionsAndLowercaseIds() throws IOException {
        String s = read("src/main/java/dev/councilcraft/supporter/Supporters.java");
        assertTrue(s.contains("CommandManager.argument(\"name\", StringArgumentType.word())"));
        assertTrue(s.contains(".suggests((ctx, b) -> CommandSource.suggestMatching(SupporterRules.NAMED_COLORS.keySet(), b))"));
        assertTrue(s.contains("SupporterRules.parseRoleSpec(StringArgumentType.getString(ctx, \"name\"),"));
        assertEquals(2, s.split("String id = SupporterRules.roleId\\(StringArgumentType.getString\\(ctx, \"id\"\\)\\);", -1).length - 1);
        for (String sub : new String[] {"create", "assign", "remove", "delete", "list"}) {
            assertTrue(s.contains("CommandManager.literal(\"" + sub + "\")"), sub);
        }
    }
}
