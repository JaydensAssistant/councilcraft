package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/**
 * S221 / S244. Caps the speed value vanilla is about to use for movement.
 * That value is in attribute units. 0.44 sits just above a top-roll horse
 * in netherite shoes (~0.4375) and below ship and train speeds.
 */
public final class HorseSpeed {
    private HorseSpeed() {
    }

    public static float cap(float speed) {
        return Math.min(speed, CouncilCraftRules.HORSE_SPEED_CAP);
    }
}
