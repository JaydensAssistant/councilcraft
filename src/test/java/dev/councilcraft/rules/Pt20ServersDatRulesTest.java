package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** PT20: default servers.dat must ship in both mrpacks; build_release.sh makes the step unskippable. */
class Pt20ServersDatRulesTest {
    private static final String ENTRY = "client-overrides/servers.dat";
    private static final Pattern MOD_VERSION = Pattern.compile("(?m)^mod_version=(.+)$");

    private static String modVersion() throws IOException {
        Matcher m = MOD_VERSION.matcher(Files.readString(Path.of("gradle.properties")));
        assertTrue(m.find(), "mod_version in gradle.properties");
        return m.group(1).trim();
    }

    /** True when ver is strictly after 0.4.26 (so existing 0.4.25/0.4.26 dist artifacts stay green). */
    static boolean after0426(String ver) {
        String[] p = ver.split("\\.");
        if (p.length < 3) {
            return false;
        }
        int major = Integer.parseInt(p[0].replaceAll("[^0-9].*$", ""));
        int minor = Integer.parseInt(p[1].replaceAll("[^0-9].*$", ""));
        int patch = Integer.parseInt(p[2].replaceAll("[^0-9].*$", ""));
        if (major != 0) {
            return major > 0;
        }
        if (minor != 4) {
            return minor > 4;
        }
        return patch > 26;
    }

    @Test
    void buildReleaseScriptRunsAndVerifiesServersDat() throws IOException {
        Path script = Path.of("tools/build_release.sh");
        assertTrue(Files.isRegularFile(script), "tools/build_release.sh missing");
        String body = Files.readString(script);
        assertTrue(body.contains("servers_dat.py"), "build_release.sh must call servers_dat.py");
        assertTrue(body.contains("--verify"), "build_release.sh must verify servers.dat");
        assertTrue(Files.isRegularFile(Path.of("tools/servers_dat.py")));
    }

    @Test
    void distMrpacksIncludeServersDatWhenVersionAfter0426() throws IOException {
        String ver = modVersion();
        if (!after0426(ver)) {
            // 0.4.25/0.4.26 shipped without servers.dat; do not fail on those artifacts.
            return;
        }
        Path dist = Path.of("dist", ver);
        if (!Files.isDirectory(dist)) {
            return;
        }
        int checked = 0;
        try (Stream<Path> s = Files.list(dist)) {
            for (Path mr : (Iterable<Path>) s.filter(p -> p.getFileName().toString().endsWith(".mrpack"))::iterator) {
                try (ZipFile z = new ZipFile(mr.toFile())) {
                    ZipEntry e = z.getEntry(ENTRY);
                    assertNotNull(e, ENTRY + " missing from " + mr);
                    checked++;
                }
            }
        }
        assertTrue(checked >= 2, "expected both mrpacks under " + dist + ", checked " + checked);
    }
}
