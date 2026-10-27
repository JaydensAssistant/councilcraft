package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.respawn.PlayerActivity;
import dev.councilcraft.respawn.RebornMotion;
import dev.councilcraft.respawn.RebornWindow;
import dev.councilcraft.respawn.RebornWindow.Action;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** PT41 amendment 3: the all-player activity flag uses Reborn's test, and Reborn growth is unchanged. */
class PlayerActivityTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    private static String sha256(String text) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static void lookAround(RebornWindow window) {
        window.look(0f, 0f);
        window.look(20f, 0f);
        window.look(0f, 15f);
    }

    /** Runs a clock to its first boundary; returns the verdict and checks every earlier tick is mid-window. */
    private static boolean runWindow(PlayerActivity.Clock clock, long start) {
        long ticks = PlayerActivity.Clock.windowTicks();
        for (long t = 1; t < ticks; t++) {
            assertNull(clock.tick(start + t));
        }
        Boolean verdict = clock.tick(start + ticks);
        assertTrue(verdict != null);
        return verdict;
    }

    @Test
    void activeWindowIsFlagged() {
        PlayerActivity.Clock clock = new PlayerActivity.Clock();
        clock.selfMoved(100);
        lookAround(clock.window());
        clock.window().act(Action.BREAK, 1, 200);
        assertTrue(runWindow(clock, 0));
    }

    @Test
    void afkWindowIsNotFlagged() {
        assertFalse(runWindow(new PlayerActivity.Clock(), 0));
        // Camera only, no interaction: AFK, exactly like a Reborn window.
        PlayerActivity.Clock lookOnly = new PlayerActivity.Clock();
        lookOnly.selfMoved(10);
        lookAround(lookOnly.window());
        assertFalse(runWindow(lookOnly, 0));
        // Interaction and camera but no self-driven move: AFK (S256).
        PlayerActivity.Clock noMove = new PlayerActivity.Clock();
        lookAround(noMove.window());
        noMove.window().act(Action.USE, 3, 50);
        assertFalse(runWindow(noMove, 0));
    }

    @Test
    void eachWindowIsJudgedAfresh() {
        PlayerActivity.Clock clock = new PlayerActivity.Clock();
        clock.selfMoved(5);
        lookAround(clock.window());
        clock.window().act(Action.PLACE, 2, 7);
        assertTrue(runWindow(clock, 0));
        // Next window: nothing happened, so the flag drops.
        assertFalse(runWindow(clock, PlayerActivity.Clock.windowTicks()));
    }

    @Test
    void verdictEqualsRebornCreditForTheSameInputs() {
        boolean[][] cases = {{true, true, true}, {true, true, false}, {true, false, true}, {false, true, true}, {false, false, false}};
        for (boolean[] c : cases) {
            PlayerActivity.Clock clock = new PlayerActivity.Clock();
            RebornWindow reborn = new RebornWindow();
            if (c[0]) {
                lookAround(clock.window());
                lookAround(reborn);
            }
            if (c[1]) {
                clock.window().act(Action.ATTACK, 9, 30);
                reborn.act(Action.ATTACK, 9, 30);
            }
            long lastMove = c[2] ? 40L : -1L;
            if (c[2]) {
                clock.selfMoved(lastMove);
            }
            long end = PlayerActivity.Clock.windowTicks();
            boolean expected = RebornMotion.credit(reborn.counts(), end, lastMove);
            assertEquals(expected, runWindow(clock, 0), java.util.Arrays.toString(c));
        }
    }

    @Test
    void hooksRunFirstAndNeverTouchRebornState() throws IOException {
        String reborn = read("src/main/java/dev/councilcraft/respawn/Reborn.java");
        assertTrue(reborn.contains("public static void selfMoved(ServerPlayerEntity player) {\n        PlayerActivity.selfMoved(player);\n"));
        assertTrue(reborn.contains("public static void sampleLook(ServerPlayerEntity player) {\n        PlayerActivity.look(player);\n"));
        assertTrue(reborn.contains("RebornWindow.Action action, int target) {\n        PlayerActivity.note(player, action, target);\n"));
        assertTrue(reborn.contains("PlayerActivity.register();"));
        String activity = read("src/main/java/dev/councilcraft/respawn/PlayerActivity.java");
        for (String forbidden : new String[] {"MINUTES", "WINDOWS", "GROWTH", "RebornProgress", "Reborn.", "LAST_MOVE"}) {
            assertFalse(activity.contains(forbidden), forbidden);
        }
        assertTrue(activity.contains("public static final String TAG = \"cc_active\";"));
    }

    /** SHA-256 of the 0.4.17 sources (git show v0.4.17:...). Reborn growth code is byte-identical. */
    @Test
    void rebornGrowthCodeIsUnchangedFrom0417() throws Exception {
        String reborn = read("src/main/java/dev/councilcraft/respawn/Reborn.java");
        String tick = reborn.substring(reborn.indexOf("    private static void tick(MinecraftServer server) {"),
            reborn.indexOf("    /** Sends the effect."));
        assertEquals("177e0c71d1bb605fc708ee58a999b957f5f9d81a2abe4ac1eea1375f85096127", sha256(tick));
        assertEquals("5697ff35f82062c56cc4110b08f32d0813889165834c3dc597e15db7140d89b6",
            sha256(read("src/main/java/dev/councilcraft/respawn/RebornWindow.java")));
        assertEquals("781d67b2287815975493cd7bfa0a41e37394c99bdf4255e13ef7f714d7a4e1a5",
            sha256(read("src/main/java/dev/councilcraft/respawn/RebornMotion.java")));
        assertEquals("c6a307cb9ec00d5ac7778e13868b5093d0fd8bec187c7d635c0fc031aec2ce89",
            sha256(read("src/main/java/dev/councilcraft/respawn/RebornProgress.java")));
        assertEquals("7edf8c3de5247af2b9f8b76ef8280c79e0de9b379a721d6b57bc573ab2a7483e",
            sha256(read("src/main/java/dev/councilcraft/respawn/RebornActivity.java")));
    }
}
