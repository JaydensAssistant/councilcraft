package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.print.PrintPricing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 0.4.17: PT15-PT24 wiring that unit tests can check without a world. */
class Release0417RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void playerCarryingIsOnAndGuarded() throws IOException {
        assertTrue(read("pack/config/carryon-common.json").contains("\"pickupPlayers\": true,"));
        assertTrue(read("src/main/resources/councilcraft.mixins.json").contains("\"CarryEscapeMixin\""));
        assertTrue(read("src/main/java/dev/councilcraft/CouncilCraft.java").contains("dev.councilcraft.carry.BabyCarry.register();"));
        String carry = read("src/main/java/dev/councilcraft/carry/BabyCarry.java");
        assertTrue(carry.contains("if (Reborn.running(carrier) || !Reborn.running(baby))"));
        assertTrue(carry.contains("UseEntityCallback.EVENT.addPhaseOrdering(BEFORE_CARRY_ON, Event.DEFAULT_PHASE);"));
        assertTrue(read("src/main/resources/assets/councilcraft/lang/en_us.json")
            .contains("\"councilcraft.carry.cooldown\": \"Can't pick them up for %s\""));
    }

    @Test
    void dciGuardsArePinnedTo307() throws IOException {
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        assertTrue(plugin.contains("{\"dev.councilcraft.mixin.DciLeaveGuardMixin\", \"dcintegration-fabric\", \"3.0.7\","));
        assertTrue(plugin.contains("{\"dev.councilcraft.mixin.DciCommandGuardMixin\", \"dcintegration-fabric\", \"3.0.7\","));
    }

    @Test
    void feedingRefusalsHaveMessages() throws IOException {
        String lang = read("src/main/resources/assets/councilcraft/lang/en_us.json");
        assertTrue(lang.contains("\"councilcraft.feed.refused.too_far\": \"Too far to feed %s. Get within 3 blocks.\""));
        assertTrue(lang.contains("\"councilcraft.feed.refused.full\": \"%s is full.\""));
    }

    @Test
    void printerNamesBothPhotoInks() throws IOException {
        String lang = read("src/main/resources/assets/councilcraft/lang/en_us.json");
        assertTrue(lang.contains("\"item.councilcraft.process_ink_bucket\": \"Process Ink Bucket\""));
        assertTrue(lang.contains("Black Dye: black-and-white photos"));
        assertTrue(lang.contains("Process Ink: color photos and albums"));
        assertFalse(lang.contains("warning"));
    }

    @Test
    void pvpHeadsAvoidRhinoInvisibleGetUuid() throws IOException {
        // PT24: Entity#getUUID is not visible to Rhino on this KubeJS ("Cannot find function getUUID").
        String js = read("pack/kubejs/server_scripts/councilcraft_heads.js");
        assertFalse(js.contains("getUUID()"));
        assertTrue(js.contains("if (direct.equals(attacker)) return attacker"));
        assertTrue(js.contains("rec.putString('uuid', String(attacker.getGameProfile().getId()))"));
    }

    @Test
    void carryEscapeCountsOnlyARealSneakDismount() throws IOException {
        String mixin = read("src/main/java/dev/councilcraft/mixin/CarryEscapeMixin.java");
        assertTrue(mixin.contains("method = \"tickRiding\""));
        assertTrue(mixin.contains("if (carried && !self.hasVehicle())"));
    }

    @Test
    void versionIs0417() throws IOException {
        assertTrue(read("gradle.properties").contains("mod_version=0.4.17\n"));
        assertTrue(read("pack/pack.toml").contains("version = \"0.4.17\""));
    }

    // PT29: thousands separators, independent of the JVM locale.
    @Test
    void copyCostsUseThousandsSeparators() {
        assertEquals("200", PrintPricing.format(200));
        assertEquals("3,360", PrintPricing.format(PrintPricing.albumCost(PrintPricing.ALBUM_PAGES, PrintPricing.ALBUM_PAGES)));
        assertEquals("1,000,000", PrintPricing.format(1_000_000));
    }

    // PT25 / PT28: one rule for "fits in the printer" (CEI's isValid: 0 < cost <= capacity).
    @Test
    void fitsPrinterMatchesCeiCapacityRule() {
        assertTrue(PrintPricing.fitsPrinter(200, 4000));
        assertTrue(PrintPricing.fitsPrinter(4000, 4000));
        assertFalse(PrintPricing.fitsPrinter(4001, 4000));
        assertFalse(PrintPricing.fitsPrinter(0, 4000));
        // S177 blocked levels carry a 1,000,000 custom cost, which never fits the 4,000 mB printer.
        assertFalse(PrintPricing.fitsPrinter(1_000_000, 4000));
    }

    @Test
    void s177BlockedLevelsHaveProhibitiveCustomCost() throws IOException {
        String costs = read("pack/moonlight-global-datapacks/councilcraft-data/data/create_enchantment_industry/data_maps/enchantment/printing/enchanted_book/custom_cost.json");
        for (Map.Entry<String, Integer> e : Map.of("minecraft:sharpness", 6, "minecraft:power", 6,
                "minecraft:blast_protection", 5, "minecraft:protection", 5).entrySet()) {
            int at = costs.indexOf("\"" + e.getKey() + "\"");
            assertTrue(at >= 0, e.getKey());
            String block = costs.substring(at, costs.indexOf("]", at));
            assertTrue(block.contains("\"level\": " + e.getValue()) && block.contains("\"value\": 1000000"), e.getKey());
        }
        String server = read("pack/config/create_enchantment_industry-server.toml");
        assertTrue(server.contains("printerFluidCapacity = 4000"));
        assertTrue(server.contains("enableEnchantedBookPrinting = true"));
    }

    @Test
    void copyCostTooltipStringsAndWiring() throws IOException {
        String lang = read("src/main/resources/assets/councilcraft/lang/en_us.json");
        assertTrue(lang.contains("\"tooltip.councilcraft.copy.black_dye\": \"Copy: %s mB Black Dye\""));
        assertTrue(lang.contains("\"tooltip.councilcraft.copy.process_ink\": \"Copy: %s mB Process Ink\""));
        assertTrue(lang.contains("\"tooltip.councilcraft.copy.experience\": \"Copy: %s mB Liquid Experience (%s XP)\""));
        assertTrue(lang.contains("\"tooltip.councilcraft.copy.cant\": \"Can't be copied\""));
        assertTrue(lang.contains("\"tooltip.councilcraft.copy.unsigned\": \"Copy: locked until signed\""));
        assertTrue(lang.contains("\"gui.councilcraft.jei.printing.album_cost\": \"%s mB per photo page, plus %s mB per note page\""));
        assertTrue(read("src/main/java/dev/councilcraft/CouncilCraftClient.java").contains("dev.councilcraft.print.CopyCostTooltip.register();"));
        String tip = read("src/main/java/dev/councilcraft/print/CopyCostTooltip.java");
        // Shared pricing: the tooltip runs the printer's own create() and CEI's, never its own formula.
        assertTrue(tip.contains("PhotoPrintingBehaviour.create(null, null, stack)"));
        assertTrue(tip.contains("SignedAlbumPrintingBehaviour.create(null, null, stack)"));
        assertTrue(tip.contains("EnchantedBookPrintingBehaviour.create(world, null, stack)"));
        assertTrue(tip.contains("enableEnchantedBookPrinting.get()"));
        assertFalse(tip.contains("* PrintPricing.PHOTO_MB") || tip.contains("PrintPricing.albumCost("));
        assertTrue(read("src/main/resources/councilcraft.client.mixins.json").contains("\"EnchantedBookCostAccess\""));
    }

    @Test
    void goggleCostShowsForAnyTank() throws IOException {
        String fluids = read("src/main/java/dev/councilcraft/print/PrintFluids.java");
        String goggles = fluids.substring(fluids.indexOf("static void goggles("));
        assertFalse(goggles.contains("return;"), "PT25: the cost line must not be skipped for an empty or wrong tank");
        assertTrue(goggles.contains("gui.goggles.printing.cost"));
        assertTrue(goggles.contains("gui.goggles.printing.incorrect_liquid"));
    }

    // PT17: pinned to Punchy 2.8g, applied only when Punchy is loaded, and never fatal.
    @Test
    void punchyAlbumMixinIsPinnedAndOptional() throws IOException {
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        assertTrue(plugin.contains("{\"dev.councilcraft.mixin.client.PunchyAlbumEquipMixin\", \"punchy\", \"2.8g\"}"));
        assertTrue(plugin.contains("{\"dev.councilcraft.mixin.client.PunchyAlbumEquipMixin\", \"punchy\"}"));
        assertTrue(read("src/main/resources/councilcraft.client.mixins.json").contains("\"PunchyAlbumEquipMixin\""));
        String src = read("src/main/java/dev/councilcraft/mixin/client/PunchyAlbumEquipMixin.java");
        assertTrue(src.contains("method = \"hasItemChanged\""));
        assertTrue(src.contains("require = 0"));
        assertTrue(src.contains("new Identifier(\"exposure\", \"album\")"));
        assertTrue(read("pack/mods/punchy-fpa.pw.toml").contains("punchy-2.8g-fabric-1.20.1.jar"));
    }

    // PT19: Default Server Properties is not in the pack.
    @Test
    void defaultServerPropertiesIsNotShipped() throws IOException {
        try (var files = Files.list(Path.of("pack/mods"))) {
            files.forEach(f -> {
                String name = f.getFileName().toString().toLowerCase();
                assertFalse(name.contains("default-server-properties") || name.contains("defaultserverproperties"), name);
            });
        }
        assertFalse(read("pack/index.toml").toLowerCase().contains("default-server-properties"));
    }

    // PT30: above-max books point at Super Forging, not plain forging (which clamps to max).
    @Test
    void aboveMaxBookTooltipNamesSuperBlazeForger() throws IOException {
        String js = read("pack/kubejs/client_scripts/councilcraft_tooltips.js");
        assertTrue(js.contains("Text.gold('Apply with a Super Blaze Forger (Experience Cake)')"));
        assertFalse(js.contains("Text.gold('Apply with a Blaze Forger')"));
    }
}
