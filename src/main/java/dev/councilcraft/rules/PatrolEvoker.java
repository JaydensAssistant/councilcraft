package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * S121 / S242. A patrol rolls once, when its captain is placed.
 * A roll of 0 joins an evoker. The other three rolls do not.
 */
public final class PatrolEvoker {
    private PatrolEvoker() {
    }

    public static boolean joins(int roll) {
        return roll == 0;
    }

    public static int sides() {
        return CouncilCraftRules.PATROL_EVOKER_SIDES;
    }
}
