package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * S80. The numbers for one moment of a reborn penalty.
 * {@code elapsedMillis} is real time since the death, including time offline.
 */
public final class RebornStats {
    public final float scale;
    public final int hearts;
    public final float speed;
    public final float melee;
    public final float progress;

    private RebornStats(float scale, int hearts, float speed, float melee, float progress) {
        this.scale = scale;
        this.hearts = hearts;
        this.speed = speed;
        this.melee = melee;
        this.progress = progress;
    }

    public static RebornStats at(long elapsedMillis) {
        long elapsed = Math.max(0L, elapsedMillis);
        float progress = Math.min(1f, elapsed / (float) CouncilCraftRules.REBORN_MILLIS);
        long heartSteps = Math.min(
            CouncilCraftRules.VANILLA_HEARTS - CouncilCraftRules.REBORN_HEARTS,
            elapsed / CouncilCraftRules.REBORN_HEART_MILLIS
        );
        return new RebornStats(
            CouncilCraftRules.REBORN_SCALE + (1f - CouncilCraftRules.REBORN_SCALE) * progress,
            CouncilCraftRules.REBORN_HEARTS + (int) heartSteps,
            CouncilCraftRules.REBORN_SPEED + (1f - CouncilCraftRules.REBORN_SPEED) * progress,
            CouncilCraftRules.REBORN_MELEE + (1f - CouncilCraftRules.REBORN_MELEE) * progress,
            progress
        );
    }
}
