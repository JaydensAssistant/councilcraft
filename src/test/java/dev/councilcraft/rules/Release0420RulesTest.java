package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** 0.4.20: what unit tests can prove without a world for PT44 to PT50. */
class Release0420RulesTest {
    private static final String CIT = "assets/minecraft/citresewn/cit/printed_books/";

    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void versionIsAtLeast0420() throws IOException {
        // The exact version is asserted by the newest release test (Release0421RulesTest).
        assertTrue(read("gradle.properties").matches("(?s).*mod_version=0\\.4\\.[2-9]\\d.*"));
    }

    // PT46 -------------------------------------------------------------------------------------------------------

    private static BufferedImage png(String p) throws IOException {
        return ImageIO.read(Path.of(p).toFile());
    }

    @Test
    void cmykInkIsFlatOpaqueBands() throws IOException {
        String dir = "src/main/resources/assets/councilcraft/textures/block/";
        int[][] bands = {{0, 116, 154}, {152, 10, 90}, {174, 158, 17}, {39, 39, 46}};
        for (String name : new String[] {"process_ink_still.png", "process_ink_flow.png"}) {
            BufferedImage img = png(dir + name);
            int w = img.getWidth();
            assertEquals(w == 16 ? 512 : 1024, img.getHeight(), name + " keeps 32 frames");
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y);
                    assertEquals(255, argb >>> 24, name + " is fully opaque at " + x + "," + y);
                    int band = x * 4 / w;
                    // Grain only multiplies a near-white base (238-247 of 255), so a band pixel is 93% to 97% of the band color.
                    int[] want = bands[band];
                    int[] got = {(argb >> 16) & 255, (argb >> 8) & 255, argb & 255};
                    for (int c = 0; c < 3; c++) {
                        assertTrue(got[c] <= want[c] && got[c] >= Math.floor(want[c] * 0.93) - 1,
                            name + " band " + band + " channel " + c + " = " + got[c] + " vs " + want[c]);
                    }
                }
            }
        }
    }

    @Test
    void cmykInkRendersAsASolidFluid() throws IOException {
        String client = read("src/main/java/dev/councilcraft/CouncilCraftClient.java");
        String flat = client.replaceAll("\\s+", " ").replace("dev.councilcraft.print.", "");
        assertTrue(flat.contains("putFluids( net.minecraft.client.render.RenderLayer.getSolid(), ProcessInk.STILL, ProcessInk.FLOWING)"), flat);
        assertFalse(flat.contains("getTranslucent"));
    }

    // PT50 -------------------------------------------------------------------------------------------------------

    @Test
    void advancementInfoIsGoneAndClothConfigStays() throws IOException {
        assertFalse(Files.exists(Path.of("pack/mods/advancementinfo.pw.toml")));
        assertFalse(read("pack/index.toml").contains("advancementinfo"));
        assertTrue(Files.exists(Path.of("pack/mods/cloth-config.pw.toml")));
        assertTrue(read("pack/index.toml").contains("mods/cloth-config.pw.toml"));
    }

    // PT49 -------------------------------------------------------------------------------------------------------

    @Test
    void generationTooltipFilterIsRegisteredOnTheClient() throws IOException {
        assertTrue(read("src/main/java/dev/councilcraft/CouncilCraftClient.java")
            .contains("dev.councilcraft.print.PrintedTooltipFilter.register();"));
    }

    // PT48 -------------------------------------------------------------------------------------------------------

    private static final String[][] ICONS = {
        {"written_book_printed", "minecraft:written_book", "book"},
        {"name_tag_printed", "minecraft:name_tag", "book"},
        {"signed_album_printed", "exposure:signed_album", "book"},
        {"photograph_printed", "exposure:photograph", "photo"},
        {"aged_photograph_printed", "exposure:aged_photograph", "photo"},
    };

    private static Properties props(ZipFile zip, String name) throws IOException {
        Properties p = new Properties();
        p.load(new ByteArrayInputStream(zip.getInputStream(zip.getEntry(CIT + name + ".properties")).readAllBytes()));
        return p;
    }

    private static BufferedImage zipPng(ZipFile zip, String name) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(zip.getInputStream(zip.getEntry(CIT + name + ".png")).readAllBytes()));
    }

    @Test
    void printedCopyIconsAreItemRulesLikeTheBookRule() throws IOException {
        try (ZipFile zip = new ZipFile("pack/resourcepacks/councilcraft-printed-copies.zip")) {
            Properties book = props(zip, "printed_book_item");
            assertEquals("minecraft:enchanted_book", book.getProperty("items"));
            Set<String> names = new HashSet<>();
            zip.stream().map(ZipEntry::getName).forEach(names::add);
            for (String[] icon : ICONS) {
                Properties p = props(zip, icon[0]);
                assertEquals("item", p.getProperty("type"));
                assertEquals(icon[1], p.getProperty("items"));
                assertEquals(book.getProperty("nbt.councilcraft:printed"), p.getProperty("nbt.councilcraft:printed"));
                assertEquals("regex:.*", p.getProperty("nbt.councilcraft:printed"));
                assertEquals("./" + icon[0] + ".png", p.getProperty("texture"));
                assertEquals("10", p.getProperty("weight"));
                assertTrue(names.contains(CIT + icon[0] + ".png"));
            }
            // CIT Resewn crashes on in-world reloads with Iris when enchantment-type rules exist (PPM64): only item rules.
            for (String n : names) {
                if (n.endsWith(".properties")) {
                    Properties p = new Properties();
                    p.load(new ByteArrayInputStream(zip.getInputStream(zip.getEntry(n)).readAllBytes()));
                    assertEquals("item", p.getProperty("type"), n);
                }
            }
            assertTrue(names.contains(CIT + "CREDITS.txt"));
            String credits = new String(zip.getInputStream(zip.getEntry(CIT + "CREDITS.txt")).readAllBytes());
            assertTrue(credits.contains("mortuusars") && credits.contains("Mojang"));
        }
        String ours = read("src/main/resources/META-INF/councilcraft-texture-credits.txt");
        assertTrue(ours.contains("Printed-copy icons (PT48)") && ours.contains("mortuusars"));
    }

    private static float[] hsb(int argb) {
        return Color.RGBtoHSB((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, null);
    }

    @Test
    void booksAreMutedAndPhotographsOnlyDimmed() throws IOException {
        try (ZipFile zip = new ZipFile("pack/resourcepacks/councilcraft-printed-copies.zip");
            ZipFile exposure = new ZipFile(System.getProperty("cc.exposureJar"))) {
            for (String[] icon : ICONS) {
                if (!icon[1].startsWith("exposure:")) {
                    continue;
                }
                String src = "assets/exposure/textures/item/" + icon[1].substring("exposure:".length()) + ".png";
                BufferedImage original = ImageIO.read(new ByteArrayInputStream(exposure.getInputStream(exposure.getEntry(src)).readAllBytes()));
                BufferedImage made = zipPng(zip, icon[0]);
                assertEquals(original.getWidth(), made.getWidth());
                assertEquals(original.getHeight(), made.getHeight());
                for (int y = 0; y < original.getHeight(); y++) {
                    for (int x = 0; x < original.getWidth(); x++) {
                        int o = original.getRGB(x, y);
                        int m = made.getRGB(x, y);
                        assertEquals(o >>> 24, m >>> 24, "alpha is kept");
                        if ((o >>> 24) == 0) {
                            continue;
                        }
                        for (int shift : new int[] {16, 8, 0}) {
                            int oc = (o >> shift) & 255;
                            int mc = (m >> shift) & 255;
                            if ("photo".equals(icon[2])) {
                                // Brightness x0.75 only: no sepia tint, no desaturation.
                                assertEquals(Math.round(oc * 0.75), mc, 1, icon[0] + " " + x + "," + y);
                            }
                        }
                        if ("book".equals(icon[2]) && hsb(o)[1] > 0.3F) {
                            assertTrue(hsb(m)[1] < hsb(o)[1] * 0.7F, icon[0] + " is desaturated at " + x + "," + y);
                        }
                    }
                }
            }
        }
    }

    // PT47 / PT44 ------------------------------------------------------------------------------------------------

    @Test
    void scanTestsForPt44AndPt47Exist() {
        assertTrue(Files.exists(Path.of("src/test/java/dev/councilcraft/kubejs/KubeJsConstLoopTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/dev/councilcraft/kubejs/AmericanSpellingTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/dev/councilcraft/print/PrintedTooltipFilterTest.java")));
    }

    // PT45 -------------------------------------------------------------------------------------------------------

    @Test
    void portalFadeHasNoDestinationHold() throws IOException {
        assertFalse(Files.exists(Path.of("src/main/java/dev/councilcraft/compat/client/PortalFadeLevels.java")));
        String fog = read("src/main/java/dev/councilcraft/compat/PortalFadeDhFog.java");
        assertFalse(fog.contains("DhApiLevelLoadEvent") || fog.contains("levelLoaded"));
        String fade = read("src/main/java/dev/councilcraft/compat/client/PortalFade.java");
        assertTrue(fade.contains("CLEAR_SECONDS = 3.0"));
        assertTrue(fade.contains("if (PortalDhDepth.inPortalView())"));
        // Kept from PT13 / PT31: depth bypass, deferred fix, IP hook, end-tick backup.
        assertTrue(Files.exists(Path.of("src/main/java/dev/councilcraft/mixin/client/IpDimensionChangeFadeMixin.java")));
        assertTrue(read("src/main/java/dev/councilcraft/CouncilCraftClient.java").contains("PortalFade::onEndTick"));
        List<String> mixins = Stream.of(read("src/main/resources/councilcraft.client.mixins.json").split("\n"))
            .map(String::trim).collect(Collectors.toList());
        assertTrue(mixins.stream().anyMatch(l -> l.contains("IrisDhPortalDepthMixin")));
    }
}
