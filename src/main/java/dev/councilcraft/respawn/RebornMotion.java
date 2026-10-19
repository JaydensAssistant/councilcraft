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
 * <p>Falling, ice momentum, knockback, bubble columns, pistons, water currents
 * and teleports do not count. Riding never counts. The track is not saved.
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
    /** {@code Entity.updateMovementInFluid} speed for water. Lava is half of this. */
    public static final double WATER_PUSH = 0.014;
    /**
     * One step on blue ice (speed 0.1, slipperiness 0.989) is about 0.0223.
     * A water current adds at most {@link #WATER_PUSH}. The residual has to sit
     * above the current and below that step.
     */
    public static final double INPUT_FLOOR = (WATER_PUSH + 0.0223) / 2.0;
    /** How far above a jump impulse still counts as that jump. */
    public static final double JUMP_BAND = 0.25;
    /**
     * Ticks after knockback or a piston during which an accepted move only
     * refreshes the coast. Long enough for the impulse packet to arrive, short
     * of the ice slide that follows.
     */
    public static final long IMPULSE_TICKS = 10L;
    /** Already rising this fast, so a later upward packet is not a new jump. */
    private static final double ALREADY_RISING = 0.2;

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
     * True when {@code (dx, dz)} is not the coast {@code (coastX, coastZ)}.
     * Coast is the previous displacement after one tick of drag. Speeding up
     * or strafing counts. Slowing down, including a wall, does not: releasing
     * the key and hitting a block both look like that, and the steps before
     * them already counted.
     */
    public static boolean horizontalInput(double coastX, double coastZ, double dx, double dz) {
        double rx = dx - coastX;
        double rz = dz - coastZ;
        double coastLen = Math.hypot(coastX, coastZ);
        if (coastLen < 1.0e-8) {
            return Math.hypot(dx, dz) > INPUT_FLOOR;
        }
        double along = (rx * coastX + rz * coastZ) / coastLen;
        double cross = Math.abs(rx * coastZ - rz * coastX) / coastLen;
        return along > INPUT_FLOOR || cross > INPUT_FLOOR;
    }

    /**
     * A jump replaces vertical speed with the jump impulse as the player leaves
     * the ground. Falling and a bubble column are already moving, or they never
     * leave a standstill into that impulse.
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
     * One accepted position sample. {@code glide} is elytra or riptide: those
     * ticks never count, and neither does the first tick after them.
     */
    public static boolean sample(
        ServerPlayerEntity player,
        long tick,
        double x,
        double y,
        double z,
        boolean onGround,
        double drag,
        double jumpVelocity,
        boolean glide
    ) {
        return track(player).sample(tick, x, y, z, onGround, drag, jumpVelocity, glide);
    }

    private static Track track(ServerPlayerEntity player) {
        return TRACKS.computeIfAbsent(player, ignored -> new Track());
    }

    /**
     * Per-player coast. Not saved. A missed tick stores the gap as momentum and
     * does not credit it, so a lagging ice slide is not a step.
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
        private boolean onGround;
        private boolean gliding;

        public void reset() {
            seeded = false;
            dx = 0.0;
            dy = 0.0;
            dz = 0.0;
            suppressUntil = Long.MIN_VALUE;
            gliding = false;
        }

        public void suppressThrough(long tick) {
            if (tick > suppressUntil) {
                suppressUntil = tick;
            }
        }

        public boolean sample(
            long tick,
            double x,
            double y,
            double z,
            boolean onGround,
            double drag,
            double jumpVelocity,
            boolean glide
        ) {
            if (!seeded || tick < lastTick) {
                seed(tick, x, y, z, onGround, drag, glide);
                return false;
            }
            long gap = tick - lastTick;
            double obsX = x - this.x;
            double obsY = y - this.y;
            double obsZ = z - this.z;
            boolean credit = false;
            if (gap == 1L && tick > suppressUntil && !glide && !gliding) {
                double scale = this.drag;
                if (horizontalInput(this.dx * scale, this.dz * scale, obsX, obsZ)
                    || jumpInput(this.onGround, onGround, this.dy, obsY, jumpVelocity)) {
                    credit = true;
                }
            }
            this.dx = obsX;
            this.dy = obsY;
            this.dz = obsZ;
            this.x = x;
            this.y = y;
            this.z = z;
            this.drag = drag;
            this.onGround = onGround;
            this.lastTick = tick;
            this.gliding = glide;
            return credit;
        }

        private void seed(long tick, double x, double y, double z, boolean onGround, double drag, boolean glide) {
            seeded = true;
            lastTick = tick;
            this.x = x;
            this.y = y;
            this.z = z;
            dx = 0.0;
            dy = 0.0;
            dz = 0.0;
            this.drag = drag;
            this.onGround = onGround;
            gliding = glide;
        }
    }
}
