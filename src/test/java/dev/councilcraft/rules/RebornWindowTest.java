package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.respawn.RebornMotion;
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
            moving.act(Action.ATTACK, 3, tick);
        }
        assertFalse(moving.counts());

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

        RebornWindow withinThree = lookedAround();
        int beat = 0;
        for (int i = 0; i < CouncilCraftRules.REBORN_MACRO_ACTIONS; i++) {
            withinThree.act(Action.BREAK, i, beat);
            beat += (i % 2 == 0) ? 10 : 13;
        }
        assertFalse(withinThree.counts());

        RebornWindow varied = lookedAround();
        int tick = 0;
        for (int i = 0; i < CouncilCraftRules.REBORN_MACRO_ACTIONS; i++) {
            varied.act(Action.PLACE, i, tick);
            tick += (i % 2 == 0) ? 10 : 14;
        }
        assertTrue(varied.counts());

        // The rhythmic run is ignored. One action off that cadence still counts.
        RebornWindow withOneReal = lookedAround();
        for (int i = 0; i < CouncilCraftRules.REBORN_MACRO_ACTIONS; i++) {
            withOneReal.act(Action.BREAK, i, i * 10);
        }
        withOneReal.act(Action.PLACE, 1, CouncilCraftRules.REBORN_MACRO_ACTIONS * 10 + 40);
        assertTrue(withOneReal.counts());

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

        // Pitch that totals under 5 degrees is not the second axis.
        RebornWindow shaky = new RebornWindow();
        shaky.look(0f, 0f);
        shaky.look(40f, 2f);
        shaky.look(80f, 0f);
        shaky.act(Action.BREAK, 1, 5);
        assertFalse(shaky.counts());
    }

    @Test
    void thirtyDegreesOnBothAxesIsEnough() {
        RebornWindow enough = new RebornWindow();
        enough.look(0f, 0f);
        enough.look(20f, 10f);
        enough.act(Action.PLACE, 1, 1);
        assertTrue(enough.counts());

        RebornWindow yawOnly = new RebornWindow();
        yawOnly.look(0f, 0f);
        yawOnly.look(40f, 0f);
        yawOnly.look(0f, 0f);
        yawOnly.act(Action.PLACE, 1, 1);
        assertFalse(yawOnly.counts());

        RebornWindow pitchOnly = new RebornWindow();
        pitchOnly.look(0f, 0f);
        pitchOnly.look(0f, 40f);
        pitchOnly.act(Action.PLACE, 1, 1);
        assertFalse(pitchOnly.counts());

        RebornWindow shortLook = new RebornWindow();
        shortLook.look(0f, 0f);
        shortLook.look(20f, 9f);
        shortLook.act(Action.PLACE, 1, 1);
        assertFalse(shortLook.counts());

        // 30 degrees total, but pitch never reaches 5.
        RebornWindow jiggle = new RebornWindow();
        jiggle.look(0f, 0f);
        jiggle.look(26f, 4f);
        jiggle.act(Action.PLACE, 1, 1);
        assertFalse(jiggle.counts());
    }

    @Test
    void autoclickerGapsOfFourAndFiveAreOneHeldStream() {
        RebornWindow window = lookedAround();
        int tick = 0;
        for (int i = 0; i < 24; i++) {
            window.act(Action.ATTACK, 3, tick);
            tick += (i % 2 == 0) ? 4 : 5;
        }
        assertFalse(window.counts());
    }

    @Test
    void oneTickJitterIsAMacro() {
        RebornWindow window = lookedAround();
        int[] gaps = {10, 11, 9, 10, 11, 10, 9, 11, 10, 10, 9};
        int tick = 0;
        for (int i = 0; i < CouncilCraftRules.REBORN_MACRO_ACTIONS; i++) {
            window.act(Action.BREAK, i, tick);
            if (i < gaps.length) {
                tick += gaps[i];
            }
        }
        assertFalse(window.counts());
    }

    @Test
    void irregularHumanClicksStillCount() {
        RebornWindow window = lookedAround();
        int[] gaps = {8, 21, 6, 40, 15, 28, 11, 33, 9, 19, 27};
        int tick = 0;
        for (int i = 0; i < gaps.length + 1; i++) {
            window.act(Action.ATTACK, 3, tick);
            if (i < gaps.length) {
                tick += gaps[i];
            }
        }
        assertTrue(window.counts());
    }

    @Test
    void selfMoveIsRequiredAndExternalVelocityDoesNotCount() {
        assertFalse(RebornMotion.positionIsSelfMove(true, 0.2, 0.0, 0.0, 0.0, 0.0, 0.0));
        assertFalse(RebornMotion.positionIsSelfMove(false, 0.4, 0.0, 0.0, 0.4, 0.0, 0.0));
        assertFalse(RebornMotion.positionIsSelfMove(false, 0.001, 0.0, 0.0, 0.0, 0.0, 0.0));
        assertTrue(RebornMotion.positionIsSelfMove(false, 0.15, 0.0, 0.0, 0.0, 0.0, 0.0));
        assertTrue(RebornMotion.positionIsSelfMove(false, 0.0, 0.42, 0.0, 0.0, 0.0, 0.0));
        assertTrue(RebornMotion.positionIsSelfMove(false, 0.12, 0.0, 0.0, 0.04, 0.0, 0.0));

        long window = CouncilCraftRules.REBORN_SELF_MOVE_TICKS;
        assertFalse(RebornMotion.credit(true, 1_000L, -1L));
        assertTrue(RebornMotion.credit(true, window, 0L));
        assertFalse(RebornMotion.credit(true, window + 1L, 0L));
        assertFalse(RebornMotion.credit(false, 10L, 10L));
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
