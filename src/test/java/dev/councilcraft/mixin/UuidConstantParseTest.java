package dev.councilcraft.mixin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * CC-142A regression: every {@code UUID.fromString("...")} literal in main sources
 * must parse. Invalid strings (e.g. non-hex segments) crash mixin {@code <clinit>}
 * and take down the server when the target class loads.
 */
class UuidConstantParseTest {
    private static final Pattern FROM_STRING = Pattern.compile(
            "UUID\\.fromString\\(\\s*\"([^\"]+)\"\\s*\\)");

    @Test
    void everyUuidFromStringLiteralParses() throws IOException {
        Path main = Path.of("src/main").toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(main), "src/main missing: " + main);
        List<String> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(main)) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String text = Files.readString(p);
                    Matcher m = FROM_STRING.matcher(text);
                    while (m.find()) {
                        hits.add(main.relativize(p) + ": " + m.group(1));
                        String lit = m.group(1);
                        assertDoesNotThrow(
                                () -> UUID.fromString(lit),
                                () -> "invalid UUID.fromString literal in " + p + ": " + lit);
                    }
                } catch (IOException e) {
                    fail(e);
                }
            });
        }
        assertFalse(hits.isEmpty(), "expected at least one UUID.fromString in main sources");
        // Mixins must not use fromString at all after CC-142A (prefer nameUUIDFromBytes).
        assertTrue(
                hits.stream().noneMatch(h -> h.contains("mixin/")),
                () -> "mixin UUID.fromString still present: " + hits);
    }

    @Test
    void holdBackUuidFromNameBytesIsValidAndStable() {
        UUID a = UUID.nameUUIDFromBytes(
                "councilcraft:cc142a_hold_back".getBytes(StandardCharsets.UTF_8));
        UUID b = UUID.nameUUIDFromBytes(
                "councilcraft:cc142a_hold_back".getBytes(StandardCharsets.UTF_8));
        assertTrue(a.equals(b));
        assertDoesNotThrow(() -> UUID.fromString(a.toString()));
    }
}
