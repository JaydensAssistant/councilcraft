package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;

/**
 * S80 / S258. The numbers for one moment of a reborn penalty.
 * {@code activeMinutes} is credited play time, not wall time.
 * Size, speed, strength and hunger read {@link RebornProgress#fraction}.
 * Max health does not: it steps by one hit point every 45 minutes.
 */
public final class RebornStats {
    public final float scale;
    /** Half-hearts, so 13 hit points is 6.5. */
    public final float hearts;
    /** Stepped max health. 12 at the start, 20 when recovery is finished. */
    public final int hitPoints;
    public final float speed;
    public final float melee;
    /** Exhaustion multiplier. 1.5 at rebirth, 1 when recovery is finished. */
    public final float hunger;
    public final float progress;

    private RebornStats(
        float scale,
        float hearts,
        int hitPoints,
        float speed,
        float melee,
        float hunger,
        float progress
    ) {
        this.scale = scale;
        this.hearts = hearts;
        this.hitPoints = hitPoints;
        this.speed = speed;
        this.melee = melee;
        this.hunger = hunger;
        this.progress = progress;
    }

    public static RebornStats at(long activeMinutes) {
        float progress = RebornProgress.fraction(activeMinutes);
        int hitPoints = hitPoints(activeMinutes);
        return new RebornStats(
            CouncilCraftRules.REBORN_SCALE + (1f - CouncilCraftRules.REBORN_SCALE) * progress,
            hitPoints / 2f,
            hitPoints,
            CouncilCraftRules.REBORN_SPEED + (1f - CouncilCraftRules.REBORN_SPEED) * progress,
            CouncilCraftRules.REBORN_MELEE + (1f - CouncilCraftRules.REBORN_MELEE) * progress,
            CouncilCraftRules.REBORN_HUNGER + (1f - CouncilCraftRules.REBORN_HUNGER) * progress,
            progress
        );
    }

    /** 12 HP plus one point per 45 credited minutes, capped at 20. */
    public static int hitPoints(long activeMinutes) {
        long minutes = Math.max(0L, activeMinutes);
        int gained = (int) Math.min(
            CouncilCraftRules.VANILLA_HP - CouncilCraftRules.REBORN_BASE_HP,
            minutes / CouncilCraftRules.REBORN_HEART_MINUTES
        );
        return CouncilCraftRules.REBORN_BASE_HP + gained;
    }

    /** Positive exhaustion is multiplied. Zero and negative amounts are left alone. */
    public static float exhaustion(float exhaustion, float multiplier) {
        if (!(exhaustion > 0f)) {
            return exhaustion;
        }
        return exhaustion * multiplier;
    }
}
