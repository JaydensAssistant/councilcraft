package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * S256. A window counts only when the player also moved on their own in the
 * last thirty minutes. Sprint and sneak are key packets. Walk, jump and swim
 * are a position change the server's velocity does not already explain.
 * Riding, and being carried, never count. The only state is the last tick.
 */
public final class RebornMotion {
    private RebornMotion() {
    }

    /**
     * True when this position packet is a walk, jump or swim. {@code riding}
     * covers a minecart, a boat, a horse and a Create train seat.
     */
    public static boolean positionIsSelfMove(
        boolean riding,
        double dx,
        double dy,
        double dz,
        double vx,
        double vy,
        double vz
    ) {
        if (riding) {
            return false;
        }
        double floor = CouncilCraftRules.REBORN_SELF_MOVE_BLOCKS;
        double moved = dx * dx + dy * dy + dz * dz;
        if (moved <= floor * floor) {
            return false;
        }
        double rx = dx - vx;
        double ry = dy - vy;
        double rz = dz - vz;
        return rx * rx + ry * ry + rz * rz > floor * floor;
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
}
