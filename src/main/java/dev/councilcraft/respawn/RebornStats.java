package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * S80. The numbers for one moment of a reborn penalty.
 * {@code activeTicks} is credited play time, not wall time.
 * {@code progress} comes from {@link RebornProgress}.
 */
public final class RebornStats {
    public final float scale;
    public final int hearts;
    public final float speed;
    public final float melee;
    /** Exhaustion multiplier. 1.5 at rebirth, 1 when recovery is finished. */
    public final float hunger;
    public final float progress;

    private RebornStats(float scale, int hearts, float speed, float melee, float hunger, float progress) {
        this.scale = scale;
        this.hearts = hearts;
        this.speed = speed;
        this.melee = melee;
        this.hunger = hunger;
        this.progress = progress;
    }

    public static RebornStats at(long activeTicks) {
        long ticks = Math.max(0L, activeTicks);
        float progress = RebornProgress.fraction(ticks);
        int heartSteps = (int) Math.min(
            CouncilCraftRules.VANILLA_HEARTS - CouncilCraftRules.REBORN_HEARTS,
            ticks / CouncilCraftRules.REBORN_HEART_TICKS
        );
        return new RebornStats(
            CouncilCraftRules.REBORN_SCALE + (1f - CouncilCraftRules.REBORN_SCALE) * progress,
            CouncilCraftRules.REBORN_HEARTS + heartSteps,
            CouncilCraftRules.REBORN_SPEED + (1f - CouncilCraftRules.REBORN_SPEED) * progress,
            CouncilCraftRules.REBORN_MELEE + (1f - CouncilCraftRules.REBORN_MELEE) * progress,
            CouncilCraftRules.REBORN_HUNGER + (1f - CouncilCraftRules.REBORN_HUNGER) * progress,
            progress
        );
    }

    /** Positive exhaustion is multiplied. Zero and negative amounts are left alone. */
    public static float exhaustion(float exhaustion, float multiplier) {
        if (!(exhaustion > 0f)) {
            return exhaustion;
        }
        return exhaustion * multiplier;
    }
}
