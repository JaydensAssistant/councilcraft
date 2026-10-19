package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.respawn.RebornWindow;
import dev.councilcraft.respawn.RebornWindow.Action;
import org.junit.jupiter.api.Test;

/** The five-minute activity window. No Minecraft bootstrap. */
class RebornWindowTest {
    @Test
    void cameraOnlyFails() {
        assertFalse(lookedAround().counts());
    }

    @Test
    void actionOnlyFails() {
        RebornWindow window = new RebornWindow();
        window.act(Action.BREAK, 1, 0);
        assertFalse(window.counts());
    }

    @Test
    void cameraAndActionPasses() {
        RebornWindow window = lookedAround();
        window.act(Action.BREAK, 1, 10);
        assertTrue(window.counts());

        RebornWindow singleUse = lookedAround();
        singleUse.act(Action.USE, 4, 0);
        assertTrue(singleUse.counts());
    }

    @Test
    void heldUseStreamRejected() {
        RebornWindow window = lookedAround();
        for (int tick = 0; tick < 40; tick++) {
            window.act(Action.USE, 7, tick);
        }
        assertFalse(window.counts());

        RebornWindow heldAttack = lookedAround();
        for (int tick = 0; tick < 20; tick++) {
            heldAttack.act(Action.ATTACK, 3, tick);
        }
        assertFalse(heldAttack.counts());

        RebornWindow moving = lookedAround();
        for (int tick = 0; tick < 5; tick++) {
            moving.act(Action.ATTACK, 3, tick, false);
        }
        assertTrue(moving.counts());

        // A real break still counts. The held stream is not the qualifying action.
        RebornWindow withBreak = lookedAround();
        for (int tick = 0; tick < 40; tick++) {
            withBreak.act(Action.USE, 7, tick);
        }
        withBreak.act(Action.BREAK, 1, 50);
        assertTrue(withBreak.counts());
    }

    @Test
    void rhythmicMacroRejected() {
        RebornWindow steady = lookedAround();
        for (int i = 0; i < CouncilCraftRules.REBORN_MACRO_ACTIONS; i++) {
            steady.act(Action.BREAK, i, i * 10);
        }
        assertFalse(steady.counts());

        RebornWindow withinTwo = lookedAround();
        int beat = 0;
        for (int i = 0; i < CouncilCraftRules.REBORN_MACRO_ACTIONS; i++) {
            withinTwo.act(Action.BREAK, i, beat);
            beat += (i % 2 == 0) ? 10 : 12;
        }
        assertFalse(withinTwo.counts());

        RebornWindow varied = lookedAround();
        int tick = 0;
        for (int i = 0; i < CouncilCraftRules.REBORN_MACRO_ACTIONS; i++) {
            varied.act(Action.PLACE, i, tick);
            tick += (i % 2 == 0) ? 10 : 20;
        }
        assertTrue(varied.counts());

        // One gesture, even if the screen handler reports it as many clicks.
        RebornWindow burst = lookedAround();
        for (int i = 0; i < 20; i++) {
            burst.act(Action.MENU, i, 5);
        }
        assertTrue(burst.counts());
    }

    @Test
    void eatingRejected() {
        RebornWindow window = lookedAround();
        window.act(Action.EAT, 1, 0);
        window.act(Action.EAT, 1, 8);
        window.act(Action.CHAT, 1, 16);
        assertFalse(window.counts());
    }

    @Test
    void constantSpinDoesNotCount() {
        RebornWindow window = new RebornWindow();
        window.look(0f, 0f);
        window.look(90f, 0f);
        window.look(180f, 0f);
        window.act(Action.BREAK, 1, 5);
        assertFalse(window.counts());

        // A fraction of a degree of pitch noise is not a second direction.
        RebornWindow shaky = new RebornWindow();
        shaky.look(0f, 0f);
        shaky.look(40f, 0.2f);
        shaky.look(80f, 0f);
        shaky.act(Action.BREAK, 1, 5);
        assertFalse(shaky.counts());
    }

    @Test
    void thirtyDegreesInTwoDirectionsIsEnough() {
        RebornWindow enough = new RebornWindow();
        enough.look(0f, 0f);
        enough.look(15f, 0f);
        enough.look(0f, 0f);
        enough.act(Action.PLACE, 1, 1);
        assertTrue(enough.counts());

        RebornWindow shortLook = new RebornWindow();
        shortLook.look(0f, 0f);
        shortLook.look(14.5f, 0f);
        shortLook.look(0f, 0f);
        shortLook.act(Action.PLACE, 1, 1);
        assertFalse(shortLook.counts());
    }

    @Test
    void windowEndsAfterFiveMinutes() {
        RebornWindow window = new RebornWindow();
        for (long tick = 1; tick < CouncilCraftRules.REBORN_WINDOW_TICKS; tick++) {
            assertFalse(window.advance());
        }
        assertTrue(window.advance());
    }

    private static RebornWindow lookedAround() {
        RebornWindow window = new RebornWindow();
        window.look(0f, 0f);
        window.look(20f, 0f);
        window.look(0f, 15f);
        return window;
    }
}
