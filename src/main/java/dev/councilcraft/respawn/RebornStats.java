package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * S80. The numbers for one moment of a reborn penalty.
 * {@code activeMinutes} is credited play time, not wall time.
 * {@code progress} comes from {@link RebornProgress}. Hearts use that same fraction.
 */
public final class RebornStats {
    public final float scale;
    public final float hearts;
    public final float speed;
    public final float melee;
    /** Exhaustion multiplier. 1.5 at rebirth, 1 when recovery is finished. */
    public final float hunger;
    public final float progress;

    private RebornStats(float scale, float hearts, float speed, float melee, float hunger, float progress) {
        this.scale = scale;
        this.hearts = hearts;
        this.speed = speed;
        this.melee = melee;
        this.hunger = hunger;
        this.progress = progress;
    }

    public static RebornStats at(long activeMinutes) {
        float progress = RebornProgress.fraction(activeMinutes);
        float hearts = CouncilCraftRules.REBORN_HEARTS
            + (CouncilCraftRules.VANILLA_HEARTS - CouncilCraftRules.REBORN_HEARTS) * progress;
        return new RebornStats(
            CouncilCraftRules.REBORN_SCALE + (1f - CouncilCraftRules.REBORN_SCALE) * progress,
            hearts,
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
