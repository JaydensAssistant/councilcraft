package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.respawn.RebornMotion;
import dev.councilcraft.respawn.RebornMotion.Move;
import dev.councilcraft.respawn.RebornMotion.Track;
import dev.councilcraft.respawn.RebornStats;
import org.junit.jupiter.api.Test;

/**
 * The coast is the previous accepted step times vanilla drag. A movement key
 * adds acceleration on top of that. Currents, gravity, ice and an impulse do not.
 * The key and the jump shrink with the Reborn size and speed.
 */
class RebornMotionTest {
    private static final double STONE = 0.6;
    private static final double ICE = 0.98;
    private static final double BLUE_ICE = 0.989;
    private static final double SPEED = 0.1;
    private static final double JUMP = 0.42;

    @Test
    void aMinuteZeroStepIsSmallerThanAWaterCurrent() {
        RebornStats start = RebornStats.at(0L);
        double ice = iceStep(start);
        double swim = swimStroke(start);
        assertTrue(ice < RebornMotion.WATER_PUSH);
        assertTrue(swim < RebornMotion.WATER_PUSH);
        assertTrue(RebornMotion.horizontalInput(0.0, 0.0, 0.0114, 0.0, 0.0, 0.0, ice));
        assertTrue(RebornMotion.horizontalInput(0.0, 0.0, 0.0118, 0.0, 0.0, 0.0, swim));
        assertFalse(RebornMotion.horizontalInput(
            0.0, 0.0, RebornMotion.WATER_PUSH, 0.0, RebornMotion.WATER_PUSH, 0.0, ice
        ));
        double scaledPush = RebornMotion.WATER_PUSH * start.scale;
        assertFalse(RebornMotion.horizontalInput(0.0, 0.0, scaledPush, 0.0, scaledPush, 0.0, ice));
    }

    @Test
    void minuteZeroAndMinute359StepsCount() {
        RebornStats start = RebornStats.at(0L);
        RebornStats late = RebornStats.at(359L);
        Walker earlyIce = new Walker(ground(start, BLUE_ICE));
        assertTrue(earlyIce.hold(1, 0.0114, 0.0));
        assertTrue(earlyIce.hold(2, 0.0114, 0.0));
        assertTrue(earlyIce.hold(3, 0.0114, 0.0));

        Walker earlySwim = new Walker(swim(start, 0.0, 0.0, 0.0, false));
        assertTrue(earlySwim.hold(1, 0.0118, 0.0));
        assertTrue(earlySwim.hold(2, 0.0118, 0.0));

        Walker lateIce = new Walker(ground(late, BLUE_ICE));
        assertTrue(lateIce.hold(1, 0.0224, 0.0));
        Walker lateSwim = new Walker(swim(late, 0.0, 0.0, 0.0, false));
        assertTrue(lateSwim.hold(1, 0.0196, 0.0));
    }

    @Test
    void minuteZeroWaterCurrentDoesNotCount() {
        RebornStats start = RebornStats.at(0L);
        double drag = RebornMotion.waterDrag(false, 0, false, false);
        Move current = Move.water(
            drag,
            iceStep(start),
            jumpOf(start),
            0.0,
            0.0,
            RebornMotion.WATER_PUSH,
            0.0,
            0.0,
            false
        );
        Walker walker = new Walker(current);
        for (int tick = 1; tick <= 40; tick++) {
            assertFalse(walker.hold(tick, 0.0, 0.0));
        }
        double scaled = RebornMotion.WATER_PUSH * start.scale;
        Move scaledCurrent = Move.water(drag, swimStroke(start), jumpOf(start), 0.0, 0.0, scaled, 0.0, 0.0, false);
        Walker small = new Walker(scaledCurrent);
        for (int tick = 1; tick <= 40; tick++) {
            assertFalse(small.hold(tick, 0.0, 0.0));
        }
    }

    @Test
    void jumpsFollowTheScaledImpulse() {
        RebornStats start = RebornStats.at(0L);
        double minute0 = jumpOf(start);
        assertTrue(minute0 < JUMP * (1.0 - RebornMotion.JUMP_BAND));
        assertFalse(RebornMotion.jumpInput(true, false, 0.0, 0.252, JUMP));
        assertTrue(RebornMotion.jumpInput(true, false, 0.0, 0.252, minute0));
        assertTrue(RebornMotion.jumpInput(true, false, 0.0, 0.336, RebornMotion.scaledJump(JUMP, 0.8)));

        Track early = new Track();
        early.sample(0, 0.0, 0.0, 0.0, true, ground(start, STONE));
        assertTrue(early.sample(1, 0.0, 0.252, 0.0, false, air(start)));

        RebornStats late = RebornStats.at(359L);
        Track grown = new Track();
        grown.sample(0, 0.0, 0.0, 0.0, true, ground(late, STONE));
        assertTrue(grown.sample(1, 0.0, jumpOf(late), 0.0, false, air(late)));
    }

    @Test
    void aStandingJumpCountsAcrossTheStillPacketGap() {
        RebornStats start = RebornStats.at(0L);
        Move ground = ground(start, STONE);
        Move air = air(start);
        Track five = new Track();
        five.sample(0, 0.0, 0.0, 0.0, true, ground);
        assertTrue(five.sample(5, 0.0, 0.252, 0.0, false, air));

        Track twenty = new Track();
        twenty.sample(0, 0.0, 0.0, 0.0, true, ground);
        assertTrue(twenty.sample(20, 0.0, 0.252, 0.0, false, air));

        Track late = new Track();
        late.sample(0, 0.0, 0.0, 0.0, true, ground);
        assertFalse(late.sample(21, 0.0, 0.252, 0.0, false, air));

        Track full = new Track();
        Move fullGround = Move.ground(RebornMotion.groundDrag(STONE), RebornMotion.groundStep(SPEED, STONE), JUMP);
        full.sample(0, 0.0, 0.0, 0.0, true, fullGround);
        assertTrue(full.sample(20, 0.0, JUMP, 0.0, false, Move.airborne(RebornMotion.AIR_DRAG, 0.02, JUMP, false)));
    }

    @Test
    void aGapWithMomentumDoesNotCount() {
        double drag = RebornMotion.groundDrag(ICE);
        double step = RebornMotion.groundStep(SPEED, ICE);
        Walker walker = new Walker(Move.ground(drag, step, JUMP));
        assertTrue(walker.hold(1, step, 0.0));
        assertFalse(walker.track.sample(6, walker.x + step, walker.y, walker.z, true, walker.move));
    }

    @Test
    void swimmingUpCountsAndABubbleColumnDoesNot() {
        RebornStats start = RebornStats.at(0L);
        double up = RebornMotion.SWIM_UP * start.scale;
        Move swim = swim(start, 0.0, 0.0, 0.0, false);
        Track rising = new Track();
        rising.sample(0, 0.0, 4.0, 0.0, false, swim);
        assertTrue(rising.sample(1, 0.0, 4.0 + up, 0.0, false, swim));
        double y = 4.0;
        double vy = 0.0;
        Track held = new Track();
        held.sample(0, 0.0, y, 0.0, false, swim);
        for (int tick = 1; tick <= 5; tick++) {
            vy += RebornMotion.SWIM_UP;
            y += vy * start.scale;
            assertTrue(held.sample(tick, 0.0, y, 0.0, false, swim));
            vy = vy * RebornMotion.WATER_VERTICAL_DRAG - RebornMotion.WATER_SINK;
        }

        Track fromRest = new Track();
        fromRest.sample(0, 0.0, 4.0, 0.0, false, swim);
        assertTrue(fromRest.sample(8, 0.0, 4.0 + up, 0.0, false, swim));

        Move bubble = swim(start, 0.0, 0.0, 0.0, true);
        Track column = new Track();
        column.sample(0, 0.0, 4.0, 0.0, false, bubble);
        assertFalse(column.sample(1, 0.0, 4.0 + up, 0.0, false, bubble));

        Track bare = new Track();
        bare.sample(0, 0.0, 4.0, 0.0, true, swim);
        double by = 4.0;
        double bvy = 0.0;
        for (int tick = 1; tick <= 20; tick++) {
            bvy = Math.min(0.7, bvy + RebornMotion.BUBBLE_UP);
            by += bvy * start.scale;
            assertFalse(bare.sample(tick, 0.0, by, 0.0, tick < 3, swim));
        }
    }

    @Test
    void walkingCreditsAndReleasingTheKeyDoesNot() {
        double drag = RebornMotion.groundDrag(STONE);
        double step = RebornMotion.groundStep(SPEED, STONE);
        Walker walker = new Walker(Move.ground(drag, step, JUMP));
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
        Walker walker = new Walker(Move.ground(drag, step, JUMP));
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
        Walker brake = new Walker(Move.ground(drag, step, JUMP));
        for (int tick = 1; tick <= 10; tick++) {
            brake.hold(tick, step, 0.0);
        }
        assertFalse(brake.hold(11, -step, 0.0));

        Walker strafe = new Walker(Move.ground(drag, step, JUMP));
        for (int tick = 1; tick <= 10; tick++) {
            strafe.hold(tick, step, 0.0);
        }
        assertTrue(strafe.hold(11, 0.0, step));
    }

    @Test
    void fallingDoesNotCredit() {
        Track track = new Track();
        Move air = Move.airborne(RebornMotion.AIR_DRAG, RebornMotion.scaledTravelStep(1.0), JUMP, false);
        double y = 80.0;
        double vy = 0.0;
        track.sample(0, 0.0, y, 0.0, true, air);
        boolean left = false;
        for (int tick = 1; y > 40.0 && tick < 200; tick++) {
            y += vy;
            vy = (vy - RebornMotion.GRAVITY) * 0.98;
            left = true;
            assertFalse(track.sample(tick, 0.0, y, 0.0, false, air));
        }
        assertTrue(left);
        assertTrue(y <= 40.0);
    }

    @Test
    void jumpCreditsOnceAndTheFallAfterItDoesNot() {
        Track track = new Track();
        Move ground = Move.ground(RebornMotion.groundDrag(STONE), RebornMotion.groundStep(SPEED, STONE), JUMP);
        Move air = Move.airborne(RebornMotion.AIR_DRAG, RebornMotion.scaledTravelStep(1.0), JUMP, false);
        track.sample(0, 0.0, 0.0, 0.0, true, ground);
        assertTrue(track.sample(1, 0.0, JUMP, 0.0, false, air));
        double y = JUMP;
        double vy = (JUMP - RebornMotion.GRAVITY) * 0.98;
        for (int tick = 2; tick <= 25; tick++) {
            y += vy;
            vy = (vy - RebornMotion.GRAVITY) * 0.98;
            assertFalse(track.sample(tick, 0.0, y, 0.0, false, air));
        }
    }

    @Test
    void knockbackOntoIceDoesNotCredit() {
        double drag = RebornMotion.groundDrag(ICE);
        Move move = Move.ground(drag, RebornMotion.groundStep(SPEED, ICE), JUMP);
        Track track = new Track();
        track.sample(0, 0.0, 0.0, 0.0, true, move);
        track.suppressThrough(RebornMotion.IMPULSE_TICKS);
        double x = 0.4;
        double y = 0.4;
        double vx = 0.4;
        double vy = 0.4;
        assertFalse(track.sample(1, x, y, 0.0, false, move));
        for (int tick = 2; tick <= 40; tick++) {
            vx *= drag;
            x += vx;
            vy = (vy - RebornMotion.GRAVITY) * 0.98;
            y += vy;
            assertFalse(track.sample(tick, x, y, 0.0, false, move));
        }
    }

    @Test
    void pistonPushAndTheStopAfterItDoNotCredit() {
        double drag = RebornMotion.groundDrag(STONE);
        double step = RebornMotion.groundStep(SPEED, STONE);
        Move move = Move.ground(drag, step, JUMP);
        Track track = new Track();
        track.sample(0, 0.0, 0.0, 0.0, true, move);
        track.suppressThrough(RebornMotion.IMPULSE_TICKS);
        assertFalse(track.sample(1, 0.69, 0.0, 0.0, true, move));
        for (int tick = 2; tick <= 15; tick++) {
            assertFalse(track.sample(tick, 0.69, 0.0, 0.0, true, move));
        }
        Walker walker = new Walker(track, move, 0.69, 0.0, 0.0, 0.0);
        assertTrue(walker.hold(16, step, 0.0));
    }

    @Test
    void waterCurrentDoesNotCreditAndSwimmingDoes() {
        double drag = RebornMotion.waterDrag(false, 0, false, false);
        Move current = Move.water(drag, 0.02, JUMP, 0.0, 0.0, RebornMotion.WATER_PUSH, 0.0, 0.0, false);
        Walker pushed = new Walker(current);
        for (int tick = 1; tick <= 40; tick++) {
            assertFalse(pushed.hold(tick, 0.0, 0.0));
        }
        Walker swim = new Walker(Move.water(drag, 0.02, JUMP, 0.0, 0.0, 0.0, 0.0, 0.0, false));
        assertTrue(swim.hold(1, 0.02, 0.0));
        assertTrue(swim.hold(2, 0.02, 0.0));
    }

    @Test
    void bubbleColumnDoesNotCredit() {
        Track rising = new Track();
        double drag = RebornMotion.waterDrag(false, 0, false, false);
        Move move = Move.water(drag, 0.02, JUMP, RebornMotion.SWIM_UP, RebornMotion.WATER_SINK, 0.0, 0.0, 0.0, true);
        double y = 0.0;
        double vy = 0.0;
        rising.sample(0, 0.0, y, 0.0, true, move);
        for (int tick = 1; tick <= 40; tick++) {
            vy = Math.min(0.7, vy + RebornMotion.BUBBLE_UP);
            y += vy;
            assertFalse(rising.sample(tick, 0.0, y, 0.0, tick < 3, move));
        }
    }

    @Test
    void teleportResetsTheCoast() {
        double drag = RebornMotion.groundDrag(STONE);
        double step = RebornMotion.groundStep(SPEED, STONE);
        Move move = Move.ground(drag, step, JUMP);
        Walker walker = new Walker(move);
        for (int tick = 1; tick <= 10; tick++) {
            walker.hold(tick, step, 0.0);
        }
        walker.track.reset();
        assertFalse(walker.track.sample(11, 1000.0, 70.0, -40.0, true, move));
        assertFalse(walker.track.sample(12, 1000.0, 70.0, -40.0, true, move));
        Walker after = new Walker(walker.track, move, 1000.0, 70.0, 0.0, 0.0);
        assertTrue(after.hold(13, step, 0.0));
    }

    @Test
    void aLaggingIceSlideDoesNotCredit() {
        double drag = RebornMotion.groundDrag(ICE);
        double step = RebornMotion.groundStep(SPEED, ICE);
        Move move = Move.ground(drag, step, JUMP);
        Walker walker = new Walker(move);
        for (int tick = 1; tick <= 20; tick++) {
            walker.hold(tick, step, 0.0);
        }
        for (int tick = 21; tick <= 25; tick++) {
            assertFalse(walker.coast(tick));
        }
        for (int missed = 0; missed < 4; missed++) {
            walker.glide();
        }
        assertFalse(walker.track.sample(29, walker.x, walker.y, walker.z, true, move));
        assertFalse(walker.coast(30));
        assertFalse(walker.coast(31));
    }

    @Test
    void glidingDoesNotCredit() {
        Track track = new Track();
        Move glide = Move.airborne(0.99, RebornMotion.scaledTravelStep(1.0), JUMP, true);
        track.sample(0, 0.0, 80.0, 0.0, false, glide);
        double x = 0.0;
        for (int tick = 1; tick <= 10; tick++) {
            x += 0.8;
            assertFalse(track.sample(tick, x, 80.0 - tick, 0.0, false, glide));
        }
        Move landed = Move.ground(RebornMotion.groundDrag(STONE), RebornMotion.groundStep(SPEED, STONE), JUMP);
        assertFalse(track.sample(11, x + 0.2, 69.0, 0.0, true, landed));
    }

    private static double movement(RebornStats stats) {
        return SPEED * stats.speed;
    }

    private static double iceStep(RebornStats stats) {
        return RebornMotion.scaledGroundStep(movement(stats), BLUE_ICE, stats.scale);
    }

    private static double swimStroke(RebornStats stats) {
        return RebornMotion.scaledSwimStroke(movement(stats), 0, false, stats.scale);
    }

    private static double jumpOf(RebornStats stats) {
        return RebornMotion.scaledJump(JUMP, stats.scale);
    }

    private static Move ground(RebornStats stats, double slip) {
        return Move.ground(
            RebornMotion.groundDrag(slip),
            RebornMotion.scaledGroundStep(movement(stats), slip, stats.scale),
            jumpOf(stats)
        );
    }

    private static Move air(RebornStats stats) {
        return Move.airborne(RebornMotion.AIR_DRAG, RebornMotion.scaledTravelStep(stats.scale), jumpOf(stats), false);
    }

    private static Move swim(RebornStats stats, double flowX, double flowY, double flowZ, boolean bubble) {
        return Move.water(
            RebornMotion.waterDrag(false, 0, false, false),
            swimStroke(stats),
            jumpOf(stats),
            RebornMotion.SWIM_UP * stats.scale,
            RebornMotion.WATER_SINK * stats.scale,
            flowX,
            flowY,
            flowZ,
            bubble
        );
    }

    /** One client tick: displacement is previous displacement times drag, plus input and flow. */
    private static final class Walker {
        final Track track;
        final Move move;
        double x;
        double y;
        double z;
        double vx;
        double vz;

        Walker(Move move) {
            this(new Track(), move, 0.0, 0.0, 0.0, 0.0);
            track.sample(0L, x, y, z, true, move);
        }

        Walker(Track track, Move move, double x, double y, double vx, double vz) {
            this.track = track;
            this.move = move;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vz = vz;
        }

        /** Advance a coasting tick without telling the track, as when packets are late. */
        void glide() {
            x += vx;
            z += vz;
            vx *= move.drag;
            vz *= move.drag;
        }

        boolean hold(long tick, double inputX, double inputZ) {
            double dx = vx + inputX + move.flowX;
            double dz = vz + inputZ + move.flowZ;
            x += dx;
            z += dz;
            vx = dx * move.drag;
            vz = dz * move.drag;
            return track.sample(tick, x, y, z, true, move);
        }

        boolean coast(long tick) {
            return hold(tick, 0.0, 0.0);
        }
    }
}
