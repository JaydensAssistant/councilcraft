package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** 0.4.26: unit-testable lock PT91 without a world. */
class Release0426RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    private static boolean exists(String p) {
        return Files.exists(Path.of(p));
    }

    @Test
    void versionIs0426() throws IOException {
        assertTrue(read("gradle.properties").contains("mod_version=0.4.26"));
        assertTrue(read("pack/pack.toml").contains("version = \"0.4.26\""));
    }

    // PT91 -------------------------------------------------------------------------------------------------------

    @Test
    void c2meRemovedFromPackIndexAndCredits() throws IOException {
        assertFalse(exists("pack/mods/c2me.pw.toml"));
        String index = read("pack/index.toml").toLowerCase();
        assertFalse(index.contains("c2me"));
        assertFalse(index.contains("concurrent chunk"));
        String credits = read("pack/credits.txt").toLowerCase();
        assertFalse(credits.contains("c2me"));
        assertFalse(credits.contains("concurrent chunk management"));
        // No shipped C2ME config either (PT79 shipped none; still none).
        assertFalse(exists("pack/config/c2me.toml"));
        assertFalse(Files.walk(Path.of("pack/config"))
            .anyMatch(p -> p.getFileName().toString().toLowerCase().contains("c2me")));
    }

    @Test
    void c2meAbsentFromBuiltMrpacksWhenPresent() throws IOException {
        Path dist = Path.of("dist/0.4.26");
        if (!Files.isDirectory(dist)) {
            // Pack/index absence covered above; mrpack checked after export in the release pipeline.
            return;
        }
        try (Stream<Path> s = Files.list(dist)) {
            for (Path mr : (Iterable<Path>) s.filter(p -> p.getFileName().toString().endsWith(".mrpack"))::iterator) {
                try (ZipFile z = new ZipFile(mr.toFile())) {
                    boolean found = z.stream()
                        .map(ZipEntry::getName)
                        .anyMatch(n -> n.toLowerCase().contains("c2me"));
                    assertFalse(found, "C2ME still in " + mr);
                    // Also check modrinth.index.json body
                    ZipEntry idx = z.getEntry("modrinth.index.json");
                    if (idx != null) {
                        String body = new String(z.getInputStream(idx).readAllBytes());
                        assertFalse(body.toLowerCase().contains("c2me"), "C2ME in index of " + mr);
                    }
                }
            }
        }
    }
}
