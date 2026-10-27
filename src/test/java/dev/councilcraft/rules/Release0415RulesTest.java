package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** 0.4.15: PT5 amendment (Sodium mipmaps), PT11, PT12 (patched E-LITE for Medium), PT13 (portal fog fade-in). */
class Release0415RulesTest {
    private static final String ELITE = "pack/shaderpacks/E-LITE shaders 5.1.3 CouncilCraft.zip";

    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void sodiumMipmapsAreOffAsADirectOverride() throws IOException {
        // PT5 amendment: shipped in pack/config (overwritten on update), not through yosbr.
        assertTrue(read("pack/config/sodium-mixins.properties").contains("\nmixin.features.textures.mipmaps=false\n"));
        assertFalse(Files.exists(Path.of("pack/config/yosbr/config/sodium-mixins.properties")));
    }

    @Test
    void highUsesDhMedium() throws IOException {
        // PT11.
        String s = read("src/main/java/dev/councilcraft/client/graphics/GraphicsPreset.java");
        assertTrue(s.contains("HIGH(\"ComplementaryUnbound_r5.9.3.zip\", \"MEDIUM\", 256),"));
        assertTrue(s.contains("DH Medium (PT11)"));
        assertFalse(s.contains("DH High"));
    }

    @Test
    void mediumShipsThePatchedElite() throws IOException {
        // PT12.
        String s = read("src/main/java/dev/councilcraft/client/graphics/GraphicsPreset.java");
        assertTrue(s.contains("MEDIUM(\"E-LITE shaders 5.1.3 CouncilCraft.zip\", \"MEDIUM\", 186),"));
        assertFalse(Files.exists(Path.of("pack/shaderpacks/lite-shaders.pw.toml")));
        assertTrue(Files.exists(Path.of("docs/patches/elite-5.1.3-councilcraft-taa.diff")));
        try (ZipFile zip = new ZipFile(ELITE)) {
            assertTrue(zip.getEntry("CHANGES-CouncilCraft.txt") != null);
            assertTrue(zip.getEntry("shaders/lib/fast_taa.glsl") != null);
            String notice = new String(zip.getInputStream(zip.getEntry("CHANGES-CouncilCraft.txt")).readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(notice.contains("GNU Lesser General Public License v3.0 or later"));
            assertTrue(notice.contains("Javier"));
            assertTrue(notice.contains("Entokito"));
        }
        // 0.4.21 (PT52) added NIGHT_BRIGHT, so 48 settings lines.
        String txt = read(ELITE + ".txt");
        assertTrue(txt.contains("\nAA_TYPE=3\n") || txt.startsWith("AA_TYPE=3\n"));
        assertEquals(48, txt.lines().filter(l -> l.contains("=")).count());
        String credits = read("pack/credits.txt");
        assertTrue(credits.contains("E-LITE shaders 5.1.3 (MakeUp edit) by Entokito, based on MakeUp - Ultra Fast by Javier Garduño (LGPL-3.0-or-later)"));
        assertTrue(credits.contains("modified by CouncilCraft"));
    }

    @Test
    void portalFadeHooksTargetIrisAndImmersivePortals() throws IOException {
        // PT13.
        String iris = read("src/main/java/dev/councilcraft/mixin/client/IrisPortalFadeUniformMixin.java");
        assertTrue(iris.contains("targets = \"net.irisshaders.iris.uniforms.CommonUniforms\""));
        assertTrue(iris.contains("method = \"generalCommonUniforms\""));
        assertTrue(iris.contains("stringValue=dhRenderDistance"));
        assertTrue(iris.contains("\"cc_portalFade\""));
        String ip = read("src/main/java/dev/councilcraft/mixin/client/IpDimensionChangeFadeMixin.java");
        assertTrue(ip.contains("targets = \"qouteall.imm_ptl.core.teleportation.ClientTeleportationManager\""));
        assertTrue(ip.contains("method = \"changePlayerDimension\", at = @At(\"TAIL\")"));
        String json = read("src/main/resources/councilcraft.client.mixins.json");
        assertTrue(json.contains("\"IrisPortalFadeUniformMixin\""));
        assertTrue(json.contains("\"IpDimensionChangeFadeMixin\""));
        String client = read("src/main/java/dev/councilcraft/CouncilCraftClient.java");
        assertTrue(client.contains("PortalFadeDhFog.register();"));
        assertTrue(client.contains("PortalFade::onEndTick"));
        // Complementary is untouched; DHCompat.getRenderDistance is not hooked.
        assertTrue(Files.exists(Path.of("pack/shaderpacks/complementary-unbound.pw.toml")));
        assertFalse(iris.contains("method = \"getRenderDistance"));
        assertFalse(iris.contains("DHCompat\""));
    }
}
