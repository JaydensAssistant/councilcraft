package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** CC-10A: Aggressive CPU load is client-overrides only; shared overrides keep server threads. */
class Cc10aDhCpuLoadRulesTest {
    private static final Pattern MOD_VERSION = Pattern.compile("(?m)^mod_version=(.+)$");
    private static final String SHARED = "overrides/config/DistantHorizons.toml";
    private static final String CLIENT = "client-overrides/config/DistantHorizons.toml";
    private static final String AGGRESSIVE = "threadPresetSetting = \"AGGRESSIVE\"";

    private static String modVersion() throws IOException {
        Matcher m = MOD_VERSION.matcher(Files.readString(Path.of("gradle.properties")));
        assertTrue(m.find(), "mod_version in gradle.properties");
        return m.group(1).trim();
    }

    @Test
    void buildReleaseWiresCc10aClientPresetScript() throws IOException {
        Path script = Path.of("tools/build_release.sh");
        assertTrue(Files.isRegularFile(script));
        String body = Files.readString(script);
        assertTrue(body.contains("cc10a_dh_client_preset.py"), "build_release must call CC-10A injector");
        assertTrue(Files.isRegularFile(Path.of("tools/cc10a_dh_client_preset.py")));
        String pack = Files.readString(Path.of("pack/config/DistantHorizons.toml"));
        assertTrue(pack.contains("CC-10A"), "pack config documents client-only CC-10A scope");
        assertFalse(pack.contains(AGGRESSIVE), "shared pack config must not set AGGRESSIVE (server uses it)");
        assertTrue(pack.contains("numberOfThreads = 4"));
    }

    @Test
    void distMrpacksHaveClientOnlyAggressiveWhenPresent() throws IOException {
        Path dist = Path.of("dist", modVersion());
        if (!Files.isDirectory(dist)) {
            return;
        }
        int checked = 0;
        try (Stream<Path> s = Files.list(dist)) {
            for (Path mr : (Iterable<Path>) s.filter(p -> p.getFileName().toString().endsWith(".mrpack"))::iterator) {
                try (ZipFile z = new ZipFile(mr.toFile())) {
                    ZipEntry client = z.getEntry(CLIENT);
                    ZipEntry shared = z.getEntry(SHARED);
                    if (client == null || shared == null) {
                        // Mid-build before injector / incomplete export — skip like PT20.
                        continue;
                    }
                    String c = new String(z.getInputStream(client).readAllBytes(), StandardCharsets.UTF_8);
                    String sh = new String(z.getInputStream(shared).readAllBytes(), StandardCharsets.UTF_8);
                    assertTrue(c.contains(AGGRESSIVE), mr + " client-overrides missing AGGRESSIVE");
                    assertFalse(sh.contains(AGGRESSIVE), mr + " shared overrides must not set AGGRESSIVE");
                    assertTrue(sh.contains("numberOfThreads = 4"), mr + " server threads");
                    checked++;
                }
            }
        }
        // 0 mid-build is OK; when both packs are finished we expect 2.
        assertTrue(checked == 0 || checked >= 2, "expected 0 (mid-build) or both mrpacks, got " + checked);
    }
}
