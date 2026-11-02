package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.respawn.RebornStats;
import dev.councilcraft.respawn.RebornText;
import java.util.List;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

/** CC-128AA: drop our Time left line; keep vanilla timer on icon-only hovers. */
public class Cc128AaRulesTest {
    @Test
    void cardOrEmiBarHoverIsSentenceAndNowOnly() {
        List<Text> hover = RebornText.hover(RebornStats.at(0L));
        assertEquals(2, hover.size());
        assertEquals(RebornText.line1().getString(), hover.get(0).getString());
        assertEquals(RebornText.now(RebornStats.at(0L)).getString(), hover.get(1).getString());
        assertFalse(hover.stream().anyMatch(t -> t.getString().startsWith("Time left")
            || t.getString().contains("time_left")));
        // Lang key still exists for stability, but hover must not use it.
        assertEquals(RebornText.TIME_LEFT, "effect.councilcraft.reborn.time_left");
    }

    @Test
    void iconOnlyKeepsVanillaNameAndDurationThenOurs() {
        List<Text> vanilla = List.of(
            Text.translatable(RebornText.NAME),
            Text.literal("1:59:59")
        );
        List<Text> out = RebornText.hoverKeepingVanillaTimer(vanilla, RebornStats.at(0L));
        assertEquals(4, out.size());
        assertEquals(vanilla.get(0).getString(), out.get(0).getString());
        assertEquals("1:59:59", out.get(1).getString());
        assertEquals(RebornText.line1().getString(), out.get(2).getString());
        assertEquals(RebornText.now(RebornStats.at(0L)).getString(), out.get(3).getString());
    }

    @Test
    void iconOnlyDedupesAcrossTwoHooks() {
        List<Text> vanilla = List.of(
            Text.translatable(RebornText.NAME),
            Text.literal("0:05")
        );
        List<Text> once = RebornText.hoverKeepingVanillaTimer(vanilla, RebornStats.at(30L));
        List<Text> twice = RebornText.hoverKeepingVanillaTimer(once, RebornStats.at(30L));
        assertEquals(once.size(), twice.size());
        assertTrue(once == twice || once.equals(twice));
        long sentenceCount = twice.stream()
            .filter(t -> t.getString().equals(RebornText.line1().getString()))
            .count();
        assertEquals(1L, sentenceCount);
    }
}
