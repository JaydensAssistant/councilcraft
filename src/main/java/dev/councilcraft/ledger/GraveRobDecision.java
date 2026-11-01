package dev.councilcraft.ledger;

import java.util.Objects;
import java.util.UUID;

/**
 * CC-155 / CC-156. Pure decision for whether a grave retrieval is a grave-rob
 * that Ledger should log. Owner retrieval is never logged; non-owner loot is.
 */
public final class GraveRobDecision {
    private GraveRobDecision() {
    }

    /** True when the actor is not the grave owner and a log entry should be written. */
    public static boolean shouldLog(UUID actorId, UUID ownerId) {
        if (actorId == null || ownerId == null) {
            return false;
        }
        return !Objects.equals(actorId, ownerId);
    }
}
