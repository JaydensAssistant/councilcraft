package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.respawn.RebornMotion;
import dev.councilcraft.respawn.RebornMotion.Track;
import org.junit.jupiter.api.Test;

/**
 * The coast is the previous accepted step times vanilla drag. A movement key
 * adds acceleration on top of that. Currents, gravity, ice and an impulse do not.
 */
class RebornMotionTest {
    private static final double STONE = 0.6;
    private static final double ICE = 0.98;
    private static final double BLUE_ICE = 0.989;
    private static final double SPEED = 0.1;
    private static final double JUMP = 0.42;

    @Test
    void floorSitsBetweenACurrentAndAStepOnBlueIce() {
        assertTrue(RebornMotion.WATER_PUSH < RebornMotion.INPUT_FLOOR);
        assertTrue(RebornMotion.groundStep(SPEED, BLUE_ICE) > RebornMotion.INPUT_FLOOR);
        assertTrue(RebornMotion.groundStep(SPEED, ICE) > RebornMotion.INPUT_FLOOR);
        assertTrue(RebornMotion.groundStep(SPEED, STONE) > RebornMotion.groundStep(SPEED, ICE));
    }

    @Test
    void walkingCreditsAndReleasingTheKeyDoesNot() {
        double drag = RebornMotion.groundDrag(STONE);
        double step = RebornMotion.groundStep(SPEED, STONE);
        Walker walker = new Walker(drag, JUMP);
        int credited = 0;
        for (int tick = 1; tick <= 20; tick++) {
            if (walker.hold(tick, step, 0.0)) {
                credited++;
            }
        }
        assertTrue(credited == 20);
        for (int tick = 21; tick <= 40; tick++) {
            assertFalse(walker.hold(tick, 0.0, 0.0));
        }
    }

    @Test
    void iceSlideAfterReleasingTheKeyDoesNotCredit() {
        double drag = RebornMotion.groundDrag(ICE);
        double step = RebornMotion.groundStep(SPEED, ICE);
        Walker walker = new Walker(drag, JUMP);
        for (int tick = 1; tick <= 30; tick++) {
            assertTrue(walker.hold(tick, step, 0.0));
        }
        for (int tick = 31; tick <= 80; tick++) {
            assertFalse(walker.hold(tick, 0.0, 0.0));
        }
    }

    @Test
    void strafeCreditsAndBrakingDoesNot() {
        double drag = RebornMotion.groundDrag(STONE);
        double step = RebornMotion.groundStep(SPEED, STONE);
        Walker brake = new Walker(drag, JUMP);
        for (int tick = 1; tick <= 10; tick++) {
            brake.hold(tick, step, 0.0);
        }
        assertFalse(brake.hold(11, -step, 0.0));

        Walker strafe = new Walker(drag, JUMP);
        for (int tick = 1; tick <= 10; tick++) {
            strafe.hold(tick, step, 0.0);
        }
        assertTrue(strafe.hold(11, 0.0, step));
    }

    @Test
    void fallingDoesNotCredit() {
        Track track = new Track();
        double y = 80.0;
        double vy = 0.0;
        track.sample(0, 0.0, y, 0.0, true, RebornMotion.AIR_DRAG, JUMP, false);
        boolean left = false;
        for (int tick = 1; y > 40.0 && tick < 200; tick++) {
            y += vy;
            vy = (vy - RebornMotion.GRAVITY) * 0.98;
            left = true;
            assertFalse(track.sample(tick, 0.0, y, 0.0, false, RebornMotion.AIR_DRAG, JUMP, false));
        }
        assertTrue(left);
        assertTrue(y <= 40.0);
    }

    @Test
    void jumpCreditsOnceAndTheFallAfterItDoesNot() {
        Track track = new Track();
        track.sample(0, 0.0, 0.0, 0.0, true, RebornMotion.groundDrag(STONE), JUMP, false);
        assertTrue(track.sample(1, 0.0, JUMP, 0.0, false, RebornMotion.AIR_DRAG, JUMP, false));
        double y = JUMP;
        double vy = (JUMP - RebornMotion.GRAVITY) * 0.98;
        for (int tick = 2; tick <= 25; tick++) {
            y += vy;
            vy = (vy - RebornMotion.GRAVITY) * 0.98;
            assertFalse(track.sample(tick, 0.0, y, 0.0, false, RebornMotion.AIR_DRAG, JUMP, false));
        }
    }

    @Test
    void knockbackOntoIceDoesNotCredit() {
        double drag = RebornMotion.groundDrag(ICE);
        Track track = new Track();
        track.sample(0, 0.0, 0.0, 0.0, true, drag, JUMP, false);
        track.suppressThrough(RebornMotion.IMPULSE_TICKS);
        double x = 0.4;
        double y = 0.4;
        double vx = 0.4;
        double vy = 0.4;
        assertFalse(track.sample(1, x, y, 0.0, false, drag, JUMP, false));
        for (int tick = 2; tick <= 40; tick++) {
            vx *= drag;
            x += vx;
            vy = (vy - RebornMotion.GRAVITY) * 0.98;
            y += vy;
            assertFalse(track.sample(tick, x, y, 0.0, false, drag, JUMP, false));
        }
    }

    @Test
    void pistonPushAndTheStopAfterItDoNotCredit() {
        Track track = new Track();
        double drag = RebornMotion.groundDrag(STONE);
        track.sample(0, 0.0, 0.0, 0.0, true, drag, JUMP, false);
        track.suppressThrough(RebornMotion.IMPULSE_TICKS);
        assertFalse(track.sample(1, 0.69, 0.0, 0.0, true, drag, JUMP, false));
        for (int tick = 2; tick <= 15; tick++) {
            assertFalse(track.sample(tick, 0.69, 0.0, 0.0, true, drag, JUMP, false));
        }
        double step = RebornMotion.groundStep(SPEED, STONE);
        Walker walker = new Walker(track, drag, JUMP, 0.69, 0.0, 0.0, 0.0);
        assertTrue(walker.hold(16, step, 0.0));
    }

    @Test
    void waterCurrentDoesNotCreditAndSwimmingDoes() {
        double drag = RebornMotion.waterDrag(false, 0, false, false);
        Walker current = new Walker(drag, JUMP);
        for (int tick = 1; tick <= 40; tick++) {
            assertFalse(current.hold(tick, RebornMotion.WATER_PUSH, 0.0));
        }
        Walker swim = new Walker(drag, JUMP);
        assertTrue(swim.hold(1, 0.02, 0.0));
        assertTrue(swim.hold(2, 0.02, 0.0));
    }

    @Test
    void bubbleColumnDoesNotCredit() {
        Track rising = new Track();
        double drag = RebornMotion.waterDrag(false, 0, false, false);
        double y = 0.0;
        double vy = 0.0;
        rising.sample(0, 0.0, y, 0.0, true, drag, JUMP, false);
        for (int tick = 1; tick <= 40; tick++) {
            vy = Math.min(0.7, vy + 0.06);
            y += vy;
            assertFalse(rising.sample(tick, 0.0, y, 0.0, tick < 3, drag, JUMP, false));
        }
    }

    @Test
    void teleportResetsTheCoast() {
        double drag = RebornMotion.groundDrag(STONE);
        double step = RebornMotion.groundStep(SPEED, STONE);
        Walker walker = new Walker(drag, JUMP);
        for (int tick = 1; tick <= 10; tick++) {
            walker.hold(tick, step, 0.0);
        }
        walker.track.reset();
        assertFalse(walker.track.sample(11, 1000.0, 70.0, -40.0, true, drag, JUMP, false));
        assertFalse(walker.track.sample(12, 1000.0, 70.0, -40.0, true, drag, JUMP, false));
        Walker after = new Walker(walker.track, drag, JUMP, 1000.0, 70.0, 0.0, 0.0);
        assertTrue(after.hold(13, step, 0.0));
    }

    @Test
    void aLaggingIceSlideDoesNotCredit() {
        double drag = RebornMotion.groundDrag(ICE);
        Walker walker = new Walker(drag, JUMP);
        double step = RebornMotion.groundStep(SPEED, ICE);
        for (int tick = 1; tick <= 20; tick++) {
            walker.hold(tick, step, 0.0);
        }
        for (int tick = 21; tick <= 25; tick++) {
            assertFalse(walker.coast(tick));
        }
        for (int missed = 0; missed < 4; missed++) {
            walker.glide();
        }
        assertFalse(walker.track.sample(29, walker.x, walker.y, walker.z, true, drag, JUMP, false));
        assertFalse(walker.coast(30));
        assertFalse(walker.coast(31));
    }

    @Test
    void glidingDoesNotCredit() {
        Track track = new Track();
        track.sample(0, 0.0, 80.0, 0.0, false, 0.99, JUMP, true);
        double x = 0.0;
        for (int tick = 1; tick <= 10; tick++) {
            x += 0.8;
            assertFalse(track.sample(tick, x, 80.0 - tick, 0.0, false, 0.99, JUMP, true));
        }
        assertFalse(track.sample(11, x + 0.2, 69.0, 0.0, true, RebornMotion.groundDrag(STONE), JUMP, false));
    }

    /** One client tick: displacement is previous displacement times drag, plus input. */
    private static final class Walker {
        final Track track;
        final double drag;
        final double jumpVelocity;
        double x;
        double y;
        double z;
        double vx;
        double vz;

        Walker(double drag, double jumpVelocity) {
            this(new Track(), drag, jumpVelocity, 0.0, 0.0, 0.0, 0.0);
            track.sample(0L, x, y, z, true, drag, jumpVelocity, false);
        }

        /** Advance a coasting tick without telling the track, as when packets are late. */
        void glide() {
            x += vx;
            z += vz;
            vx *= drag;
            vz *= drag;
        }

        Walker(Track track, double drag, double jumpVelocity, double x, double y, double vx, double vz) {
            this.track = track;
            this.drag = drag;
            this.jumpVelocity = jumpVelocity;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vz = vz;
        }

        boolean hold(long tick, double inputX, double inputZ) {
            double dx = vx + inputX;
            double dz = vz + inputZ;
            x += dx;
            z += dz;
            vx = dx * drag;
            vz = dz * drag;
            return track.sample(tick, x, y, z, true, drag, jumpVelocity, false);
        }

        boolean coast(long tick) {
            return hold(tick, 0.0, 0.0);
        }
    }
}
