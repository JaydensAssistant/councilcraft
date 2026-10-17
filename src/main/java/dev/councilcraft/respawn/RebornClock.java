package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * S80. Turns the synced expiry into elapsed real time on the client.
 * {@code serverNow} is the server clock at the moment the packet was sent.
 * {@code clientNowAtSync} is the client clock when that packet was applied.
 * Later client time is measured from there, so a wrong wall clock on the
 * client does not shift the penalty.
 */
public final class RebornClock {
    private RebornClock() {
    }

    public static long elapsed(long until, long serverNow, long clientNowAtSync, long clientNow) {
        if (until <= 0L) {
            return CouncilCraftRules.REBORN_MILLIS;
        }
        long now = serverNow + (clientNow - clientNowAtSync);
        long start = until - CouncilCraftRules.REBORN_MILLIS;
        return now - start;
    }
}
