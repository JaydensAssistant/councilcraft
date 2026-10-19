package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * S256. A window counts only when the player moved on their own in the last
 * thirty minutes. Sprint and sneak are key packets. Walk, swim and jump are
 * an accepted position change that vanilla drag does not already explain.
 * The server does not simulate a remote player's velocity, so the last
 * accepted displacement is dragged forward instead.
 *
 * <p>The step and the jump shrink with the player's Reborn size and movement
 * speed. A fixed floor cannot: a minute-0 blue-ice step is smaller than one
 * water current. The current is subtracted first. Falling, ice momentum,
 * knockback, bubble columns, pistons, water currents and teleports do not
 * count. Riding never counts. The track is not saved.
 */
public final class RebornMotion {
    /** {@code LivingEntity.travel} air friction. */
    public static final double AIR_DRAG = 0.91;
    /** Ground friction is slipperiness times this. */
    public static final double GROUND_DRAG_FACTOR = 0.91;
    /** {@code LivingEntity.travel} gravity, applied after the move. */
    public static final double GRAVITY = 0.08;
    /** Still-water horizontal drag. Sprinting uses {@link #WATER_SPRINT_DRAG}. */
    public static final double WATER_DRAG = 0.8;
    public static final double WATER_SPRINT_DRAG = 0.9;
    /** Dolphin's Grace replaces the water drag. */
    public static final double DOLPHIN_DRAG = 0.96;
    /** Depth Strider blends water drag toward the default ground drag. */
    public static final double DEPTH_STRIDER_DRAG = 0.54600006;
    public static final double LAVA_DRAG = 0.5;
    /** Water multiplies vertical speed by this, then sinks by {@link #WATER_SINK}. */
    public static final double WATER_VERTICAL_DRAG = 0.8;
    /** {@code applyFluidMovingSpeed} subtracts gravity over 16. */
    public static final double WATER_SINK = GRAVITY / 16.0;
    /** {@code swimUpward} adds this to velocity before the move. */
    public static final double SWIM_UP = 0.04;
    /** {@code onBubbleColumnCollision} adds this, which is not a swim. */
    public static final double BUBBLE_UP = 0.06;
    /** {@code LivingEntity.travel} acceleration in water, lava and air, before Depth Strider. */
    public static final double TRAVEL_ACCEL = 0.02;
    /**
     * {@code LivingEntity.tickMovement} multiplies the movement input by this
     * before {@code travel}. A single key stays under length 1, so it is not
     * normalised back up.
     */
    public static final double INPUT_ATTENUATION = 0.98;
    /** {@code Entity.updateMovementInFluid} speed for water. */
    public static final double WATER_PUSH = 0.014;
    /** Lava push in an ultrawarm dimension. Elsewhere it is {@link #LAVA_PUSH_COLD}. */
    public static final double LAVA_PUSH = 0.007;
    public static final double LAVA_PUSH_COLD = 0.0023333333333333335;
    /** How far from a jump or swim impulse still counts as that impulse. */
    public static final double JUMP_BAND = 0.25;
    /**
     * Ticks after knockback or a piston during which an accepted move only
     * refreshes the coast. Long enough for the impulse packet to arrive, short
     * of the ice slide that follows.
     */
    public static final long IMPULSE_TICKS = 10L;
    /**
     * A still client sends a position about once a second. A jump from rest
     * arrives on that cadence, not on the next tick.
     */
    public static final long STATIONARY_GAP = 20L;
    /** Already rising this fast, so a later upward packet is not a new jump. */
    private static final double ALREADY_RISING = 0.2;
    /** Stored displacement small enough that the coast has no momentum. */
    private static final double REST = 1.0e-4;

    private static final Map<ServerPlayerEntity, Track> TRACKS = new WeakHashMap<>();

    private RebornMotion() {
    }

    public static double groundDrag(double slipperiness) {
        return slipperiness * GROUND_DRAG_FACTOR;
    }

    /** Acceleration {@code LivingEntity.travel} adds for a full movement key on the ground. */
    public static double groundStep(double movementSpeed, double slipperiness) {
        double slip3 = slipperiness * slipperiness * slipperiness;
        return movementSpeed * 0.21600002 / slip3;
    }

    /**
     * One ground key, in position space. {@code movementSpeed} already includes
     * the Reborn speed modifier. {@code scale} is the Reborn size, which is also
     * the motion scale {@code move} applies.
     */
    public static double scaledGroundStep(double movementSpeed, double slipperiness, double scale) {
        return groundStep(movementSpeed, slipperiness) * INPUT_ATTENUATION * scale;
    }

    /** One water key before Depth Strider blends it toward {@code movementSpeed}. */
    public static double swimAcceleration(double movementSpeed, int depthStrider, boolean onGround) {
        double accel = TRAVEL_ACCEL;
        double depth = Math.min(Math.max(depthStrider, 0), 3);
        if (!onGround) {
            depth *= 0.5;
        }
        if (depth > 0.0) {
            accel += (movementSpeed - accel) * depth / 3.0;
        }
        return accel;
    }

    /** One swim key, in position space. */
    public static double scaledSwimStroke(double movementSpeed, int depthStrider, boolean onGround, double scale) {
        return swimAcceleration(movementSpeed, depthStrider, onGround) * INPUT_ATTENUATION * scale;
    }

    /** Air and lava use the unblended travel acceleration. */
    public static double scaledTravelStep(double scale) {
        return TRAVEL_ACCEL * INPUT_ATTENUATION * scale;
    }

    /** {@code getJumpVelocity} is unscaled. The position packet is that impulse times the motion scale. */
    public static double scaledJump(double jumpVelocity, double scale) {
        return jumpVelocity * scale;
    }

    /**
     * Water drag from {@code LivingEntity.travel}, including the sprint, Depth
     * Strider and Dolphin's Grace branches.
     */
    public static double waterDrag(boolean sprinting, int depthStrider, boolean onGround, boolean dolphinsGrace) {
        double drag = sprinting ? WATER_SPRINT_DRAG : WATER_DRAG;
        double depth = Math.min(Math.max(depthStrider, 0), 3);
        if (!onGround) {
            depth *= 0.5;
        }
        if (depth > 0.0) {
            drag += (DEPTH_STRIDER_DRAG - drag) * depth / 3.0;
        }
        if (dolphinsGrace) {
            return DOLPHIN_DRAG;
        }
        return drag;
    }

    /**
     * Drag that {@code travel} stores at the end of this tick. The next coast
     * is the displacement from this tick times this value. Water is tested
     * before the ground, matching vanilla.
     */
    public static double drag(
        boolean water,
        boolean lava,
        boolean onGround,
        boolean sprinting,
        int depthStrider,
        boolean dolphinsGrace,
        double slipperiness
    ) {
        if (water) {
            return waterDrag(sprinting, depthStrider, onGround, dolphinsGrace);
        }
        if (lava) {
            return LAVA_DRAG;
        }
        if (onGround) {
            return groundDrag(slipperiness);
        }
        return AIR_DRAG;
    }

    /**
     * True when {@code (dx, dz)} minus the fluid flow is not the coast
     * {@code (coastX, coastZ)}. Coast is the previous displacement after one
     * tick of drag. Speeding up or strafing counts. Slowing down, including a
     * wall, does not: releasing the key and hitting a block both look like
     * that, and the steps before them already counted.
     *
     * <p>{@code step} is one full key at the player's current size and speed.
     * The residual has to be at least the same share of that key as a jump
     * keeps of its impulse. There is no absolute blocks-per-tick floor.
     */
    public static boolean horizontalInput(
        double coastX,
        double coastZ,
        double dx,
        double dz,
        double flowX,
        double flowZ,
        double step
    ) {
        double need = step * (1.0 - JUMP_BAND);
        if (!(need > 0.0)) {
            return false;
        }
        double rx = dx - flowX - coastX;
        double rz = dz - flowZ - coastZ;
        double coastLen = Math.hypot(coastX, coastZ);
        if (coastLen < 1.0e-8) {
            return Math.hypot(rx, rz) >= need;
        }
        double along = (rx * coastX + rz * coastZ) / coastLen;
        double cross = Math.abs(rx * coastZ - rz * coastX) / coastLen;
        return along >= need || cross >= need;
    }

    /**
     * A jump replaces vertical speed with the jump impulse as the player leaves
     * the ground. Falling and a bubble column are already moving, or they never
     * leave a standstill into that impulse. {@code jumpVelocity} is the impulse
     * in the position packet, after the motion scale.
     */
    public static boolean jumpInput(
        boolean wasOnGround,
        boolean onGround,
        double prevDy,
        double dy,
        double jumpVelocity
    ) {
        if (!wasOnGround || onGround || !(jumpVelocity > 0.0) || prevDy > ALREADY_RISING) {
            return false;
        }
        double min = jumpVelocity * (1.0 - JUMP_BAND);
        double max = jumpVelocity * (1.0 + JUMP_BAND);
        return dy >= min && dy <= max;
    }

    /**
     * Holding jump in water adds {@code swimUp} on top of the dragged and sunk
     * previous step. {@code swimUp} is {@link #SWIM_UP} times the motion scale.
     * A bubble column adds {@link #BUBBLE_UP} and lands outside the band; the
     * caller also refuses the block itself.
     */
    public static boolean swimUpInput(double coastY, double dy, double flowY, double swimUp) {
        if (!(swimUp > 0.0)) {
            return false;
        }
        double residual = dy - flowY - coastY;
        double min = swimUp * (1.0 - JUMP_BAND);
        double max = swimUp * (1.0 + JUMP_BAND);
        return residual >= min && residual <= max;
    }

    /** True when {@code lastTick} is within the last thirty minutes of {@code now}. */
    public static boolean recent(long now, long lastTick) {
        if (lastTick < 0L || now < lastTick) {
            return false;
        }
        return now - lastTick <= CouncilCraftRules.REBORN_SELF_MOVE_TICKS;
    }

    /** Activity alone is not enough. The move has to be recent too. */
    public static boolean credit(boolean activity, long now, long lastSelfMoveTick) {
        return activity && recent(now, lastSelfMoveTick);
    }

    /** A teleport, a rejected move, or a vehicle. The next packet only reseeds. */
    public static void reset(ServerPlayerEntity player) {
        Track track = TRACKS.get(player);
        if (track != null) {
            track.reset();
        }
    }

    /** Knockback or a piston. The impulse itself must not refresh the backstop. */
    public static void pushed(ServerPlayerEntity player, long tick) {
        track(player).suppressThrough(tick + IMPULSE_TICKS);
    }

    /**
     * One accepted position sample. {@code move.glide} is elytra or riptide:
     * those ticks never count, and neither does the first tick after them.
     */
    public static boolean sample(
        ServerPlayerEntity player,
        long tick,
        double x,
        double y,
        double z,
        boolean onGround,
        Move move
    ) {
        return track(player).sample(tick, x, y, z, onGround, move);
    }

    private static Track track(ServerPlayerEntity player) {
        return TRACKS.computeIfAbsent(player, ignored -> new Track());
    }

    /**
     * What the accepted packet should be compared with. Drag, the jump impulse
     * and the step are already in position space for this player's size.
     * Flow is the fluid push in that same space.
     */
    public static final class Move {
        public final double drag;
        public final double verticalDrag;
        public final double jumpVelocity;
        public final double step;
        public final double swimUp;
        public final double sink;
        public final double flowX;
        public final double flowY;
        public final double flowZ;
        public final boolean water;
        public final boolean bubble;
        public final boolean glide;

        public Move(
            double drag,
            double verticalDrag,
            double jumpVelocity,
            double step,
            double swimUp,
            double sink,
            double flowX,
            double flowY,
            double flowZ,
            boolean water,
            boolean bubble,
            boolean glide
        ) {
            this.drag = drag;
            this.verticalDrag = verticalDrag;
            this.jumpVelocity = jumpVelocity;
            this.step = step;
            this.swimUp = swimUp;
            this.sink = sink;
            this.flowX = flowX;
            this.flowY = flowY;
            this.flowZ = flowZ;
            this.water = water;
            this.bubble = bubble;
            this.glide = glide;
        }

        public static Move ground(double drag, double step, double jumpVelocity) {
            return new Move(drag, 0.0, jumpVelocity, step, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, false);
        }

        public static Move airborne(double drag, double step, double jumpVelocity, boolean glide) {
            return new Move(drag, 0.0, jumpVelocity, step, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, glide);
        }

        public static Move water(
            double drag,
            double step,
            double jumpVelocity,
            double swimUp,
            double sink,
            double flowX,
            double flowY,
            double flowZ,
            boolean bubble
        ) {
            return new Move(
                drag,
                WATER_VERTICAL_DRAG,
                jumpVelocity,
                step,
                swimUp,
                sink,
                flowX,
                flowY,
                flowZ,
                true,
                bubble,
                false
            );
        }
    }

    /**
     * Per-player coast. Not saved. A gap with momentum is stored and not
     * credited, so a lagging ice slide is not a step. A gap from rest, up to
     * the still-client cadence, can still be a jump or a swim.
     */
    public static final class Track {
        private boolean seeded;
        private long lastTick;
        private long suppressUntil = Long.MIN_VALUE;
        private double x;
        private double y;
        private double z;
        private double dx;
        private double dy;
        private double dz;
        private double drag = AIR_DRAG;
        private double verticalDrag;
        private double sink;
        private boolean water;
        private boolean onGround;
        private boolean gliding;

        public void reset() {
            seeded = false;
            dx = 0.0;
            dy = 0.0;
            dz = 0.0;
            suppressUntil = Long.MIN_VALUE;
            gliding = false;
            water = false;
            sink = 0.0;
        }

        public void suppressThrough(long tick) {
            if (tick > suppressUntil) {
                suppressUntil = tick;
            }
        }

        public boolean sample(long tick, double x, double y, double z, boolean onGround, Move move) {
            if (!seeded || tick < lastTick) {
                seed(tick, x, y, z, onGround, move);
                return false;
            }
            long gap = tick - lastTick;
            double obsX = x - this.x;
            double obsY = y - this.y;
            double obsZ = z - this.z;
            boolean credit = false;
            boolean continuous = gap == 1L;
            boolean timely = continuous || (atRest() && gap >= 1L && gap <= STATIONARY_GAP);
            if (timely && tick > suppressUntil && !move.glide && !gliding) {
                double coastX = continuous ? this.dx * this.drag : 0.0;
                double coastZ = continuous ? this.dz * this.drag : 0.0;
                double coastY = continuous && this.water ? this.dy * this.verticalDrag - this.sink : 0.0;
                if (horizontalInput(coastX, coastZ, obsX, obsZ, move.flowX, move.flowZ, move.step)
                    || jumpInput(this.onGround, onGround, this.dy, obsY, move.jumpVelocity)
                    || (move.water && !move.bubble && swimUpInput(coastY, obsY, move.flowY, move.swimUp))) {
                    credit = true;
                }
            }
            this.dx = obsX;
            this.dy = obsY;
            this.dz = obsZ;
            this.x = x;
            this.y = y;
            this.z = z;
            remember(onGround, move);
            this.lastTick = tick;
            return credit;
        }

        private boolean atRest() {
            return Math.abs(dx) <= REST && Math.abs(dy) <= REST && Math.abs(dz) <= REST;
        }

        private void seed(long tick, double x, double y, double z, boolean onGround, Move move) {
            seeded = true;
            lastTick = tick;
            this.x = x;
            this.y = y;
            this.z = z;
            dx = 0.0;
            dy = 0.0;
            dz = 0.0;
            remember(onGround, move);
        }

        private void remember(boolean onGround, Move move) {
            this.drag = move.drag;
            this.verticalDrag = move.verticalDrag;
            this.sink = move.sink;
            this.water = move.water;
            this.onGround = onGround;
            this.gliding = move.glide;
        }
    }
}
