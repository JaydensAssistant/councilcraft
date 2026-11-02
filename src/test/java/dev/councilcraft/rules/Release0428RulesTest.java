package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** CouncilCraft 0.4.28: CC-B27 Punchy Both-hands Create blacklist + version bump. */
public class Release0428RulesTest {
    @Test
    void versionsAre0428() throws IOException {
        String props = Files.readString(Path.of("gradle.properties"));
        String pack = Files.readString(Path.of("pack/pack.toml"));
        assertTrue(props.contains("mod_version=0.4.28"));
        assertTrue(pack.contains("version = \"0.4.28\""));
    }

    @Test
    void handshakeAccepts0427And0428() {
        assertTrue(HandshakeRule.COMPATIBLE_CLIENT_VERSIONS.contains("0.4.26"));
        assertTrue(HandshakeRule.COMPATIBLE_CLIENT_VERSIONS.contains("0.4.27"));
        assertTrue(HandshakeRule.COMPATIBLE_CLIENT_VERSIONS.contains("0.4.28"));
        assertTrue(HandshakeRule.versionsMatch("0.4.28", "0.4.27"));
        assertTrue(HandshakeRule.versionsMatch("0.4.28", "0.4.28"));
        assertTrue(HandshakeRule.versionsMatch("0.4.28", "0.4.26"));
        assertFalse(HandshakeRule.versionsMatch("0.4.28", "0.4.25"));
        assertFalse(HandshakeRule.versionsMatch("0.4.28", null));
    }

    @Test
    void ccB27YosbrPunchyConfigPresent() throws IOException {
        assertTrue(Files.isRegularFile(Path.of("pack/config/yosbr/config/punchy/punchy_config.json")));
    }
}
