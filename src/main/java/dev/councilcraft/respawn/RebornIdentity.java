package dev.councilcraft.respawn;

/**
 * A reconnect can show some other effect with Reborn's remaining time.
 * The clock packet has no raw id. A duration within {@link #TICK_TOLERANCE}
 * of the remaining ticks is treated as that mis-decoded Reborn instance.
 * Ten seconds of slack covers the tick rounding in the effect packet.
 */
public final class RebornIdentity {
    public static final int TICK_TOLERANCE = 200;

    private RebornIdentity() {
    }

    public static boolean matches(int packetTicks, int expectedTicks) {
        if (packetTicks <= 0 || expectedTicks <= 0) {
            return false;
        }
        int delta = packetTicks - expectedTicks;
        if (delta < 0) {
            delta = -delta;
        }
        return delta <= TICK_TOLERANCE;
    }

    /** Same rounding the server uses when it writes the effect duration. */
    public static int ticksRemaining(long remainingMillis) {
        if (remainingMillis <= 0L) {
            return 0;
        }
        long ticks = (remainingMillis + 49L) / 50L;
        if (ticks > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.max(1L, ticks);
    }
}
