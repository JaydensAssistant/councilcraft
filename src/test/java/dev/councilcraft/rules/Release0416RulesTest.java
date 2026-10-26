package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.client.album.AlbumTextPages;
import dev.councilcraft.print.PrintPricing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 0.4.16: PT14 (+amendment), printing press and text-only album pages. */
class Release0416RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void gridCopyingIsRemovedAndAlbumUsesLeather() throws IOException {
        String js = read("pack/kubejs/server_scripts/councilcraft_recipes.js");
        assertTrue(js.contains("event.remove({ id: 'minecraft:book_cloning' })"));
        assertTrue(js.contains("event.remove({ id: 'exposure:photograph_copying' })"));
        assertTrue(js.contains("event.remove({ id: 'exposure:photograph_copying_black_and_white' })"));
        assertTrue(js.contains("event.remove({ id: 'exposure:album' })"));
        assertTrue(js.contains("event.shapeless('exposure:album', ['minecraft:writable_book', 'minecraft:leather']).id('councilcraft:exposure_album')"));
    }

    @Test
    void processInkIsMixedFromBlackDyeFluidAndThreeDyes() throws IOException {
        // Amendment 2: Create's basin has 2 input fluid tanks, so the ink is 1 fluid + 3 items, 4 dyes per 250 mB.
        String js = read("pack/kubejs/server_scripts/councilcraft_recipes.js");
        assertTrue(js.contains("type: 'create:mixing'"));
        assertTrue(js.contains("{ fluidTag: 'c:dyes/black', amount: 20250 }"));
        assertTrue(js.contains("{ item: 'minecraft:cyan_dye' }"));
        assertTrue(js.contains("{ item: 'minecraft:magenta_dye' }"));
        assertTrue(js.contains("{ item: 'minecraft:yellow_dye' }"));
        assertTrue(js.contains("results: [{ fluid: 'councilcraft:process_ink', amount: 20250 }]"));
        assertTrue(js.contains(".id('councilcraft:process_ink_mixing')"));
        String info = read("pack/kubejs/assets/councilcraft/lang/en_us.json");
        assertTrue(info.contains("mixed in a Create Mixer from 250 mB Black Dye fluid plus 1 each of Cyan, Magenta and Yellow Dye"));
    }

    @Test
    void photoCannotBeAddedOverALongNote() throws IOException {
        // Amendment 2: no photo on a page whose note is over 3 lines, so the caption box never overflows.
        assertFalse(AlbumTextPages.photoBlocked(false, 0));
        assertFalse(AlbumTextPages.photoBlocked(false, 3));
        assertTrue(AlbumTextPages.photoBlocked(false, 4));
        assertTrue(AlbumTextPages.photoBlocked(false, 14));
        assertFalse(AlbumTextPages.photoBlocked(true, 3));
        String mixin = read("src/main/java/dev/councilcraft/mixin/client/AlbumTextPageMixin.java");
        assertTrue(mixin.contains("photo.active = !blocked;"));
        assertTrue(mixin.contains("gui.councilcraft.album.hint.shorten_for_photo"));
        String lang = read("src/main/resources/assets/councilcraft/lang/en_us.json");
        assertTrue(lang.contains("\"gui.councilcraft.album.hint.shorten_for_photo\": \"Shorten the note to 3 lines to add a photo\""));
    }

    @Test
    void processInkSuppliesAFluidType() throws IOException {
        // CEI's PrinterRenderer calls getFluidType().isLighterThanAir(); a null type crashed the client.
        String src = read("src/main/java/dev/councilcraft/print/ProcessInk.java");
        assertTrue(src.contains("public FluidType getFluidType()"));
        assertTrue(src.contains("Registry.register(PortingLibFluids.FLUID_TYPES, ID"));
        assertFalse(src.contains("VirtualFluid.createSource("));
    }

    @Test
    void amendmentPricesFitThePrinterTank() {
        assertEquals(200, PrintPricing.PHOTO_MB);
        assertEquals(210, PrintPricing.albumCost(1, 1));
        assertEquals(10, PrintPricing.albumCost(0, 1));
        // Worst case: 16 captioned photo pages = 3360 mB, under CEI's 4000 mB printer tank.
        assertEquals(3360, PrintPricing.albumCost(PrintPricing.ALBUM_PAGES, PrintPricing.ALBUM_PAGES));
        assertTrue(PrintPricing.albumCost(PrintPricing.ALBUM_PAGES, PrintPricing.ALBUM_PAGES) <= 4000);
    }

    @Test
    void behavioursUseCeisPublicRegistryWithoutAMixin() throws IOException {
        String reg = read("src/main/java/dev/councilcraft/print/CouncilCraftPrinting.java");
        assertTrue(reg.contains("Registry.register(PrintingBehaviour.REGISTRY, new Identifier(\"councilcraft\", \"photograph\")"));
        assertTrue(reg.contains("Registry.register(PrintingBehaviour.REGISTRY, new Identifier(\"councilcraft\", \"signed_album\")"));
        assertTrue(read("src/main/java/dev/councilcraft/CouncilCraft.java").contains("dev.councilcraft.print.CouncilCraftPrinting.register();"));
        String photo = read("src/main/java/dev/councilcraft/print/PhotoPrintingBehaviour.java");
        assertTrue(photo.contains("if (generation >= 2)"));
        assertTrue(photo.contains("out.putInt(\"photograph_generation\", generation + 1);"));
        String album = read("src/main/java/dev/councilcraft/print/SignedAlbumPrintingBehaviour.java");
        assertTrue(album.contains("gui.councilcraft.printer.album.unsigned"));
        assertTrue(album.contains("return isBlankAlbum(stack) ? 1 : 0;"));
        String fluids = read("src/main/java/dev/councilcraft/print/PrintFluids.java");
        assertTrue(fluids.contains("new Identifier(\"c\", \"dyes/black\")"));
    }

    @Test
    void jeiPluginIsAnEntrypointAndJeiIsCompileOnly() throws IOException {
        assertTrue(read("src/main/resources/fabric.mod.json").contains("\"dev.councilcraft.print.jei.CouncilCraftJeiPlugin\""));
        assertTrue(read("build.gradle").contains("modCompileOnly(\"maven.modrinth:jei:dI7d5ZeA\") { transitive = false }"));
    }

    @Test
    void albumMixinsArePinnedToExposure1922() throws IOException {
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        for (String m : new String[] {"AlbumTextPageMixin", "AlbumViewTextPageMixin", "AlbumPhotoSlotMixin"}) {
            assertTrue(plugin.contains("client." + m), m);
            assertTrue(read("src/main/resources/councilcraft.client.mixins.json").contains("\"" + m + "\""), m);
        }
        assertTrue(plugin.contains("\"1.9.22\""));
    }

    @Test
    void blankPageArtCarriesExposuresMitNotice() throws IOException {
        assertTrue(Files.exists(Path.of("src/main/resources/assets/councilcraft/textures/gui/album_blank_page.png")));
        String credits = read("src/main/resources/META-INF/councilcraft-texture-credits.txt");
        assertTrue(credits.contains("Copyright (c) 2025 mortuusars"));
        assertTrue(read("pack/credits.txt").contains("Exposure's own album page texture by mortuusars (MIT)"));
        String lang = read("src/main/resources/assets/councilcraft/lang/en_us.json");
        assertTrue(lang.contains("\"gui.councilcraft.album.hint.add_photo\": \"Click to add photo\""));
        assertFalse(lang.contains("warning"));
    }

    @Test
    void versionIs0416() throws IOException {
        assertTrue(read("gradle.properties").contains("mod_version=0.4.16\n"));
        assertTrue(read("pack/pack.toml").contains("version = \"0.4.16\""));
    }
}
