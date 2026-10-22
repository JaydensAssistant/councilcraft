package dev.councilcraft.rules;

/**
 * PPM81. Tectonic's huge cave volume let the Overworld monster cap fill with mobs in caves far above or below every
 * player (vanilla picks the spawn Y anywhere from the world bottom to the surface). Overworld monster spawning now
 * picks Y within {@link #BAND} blocks of the horizontally nearest player, monsters outside that band of every player
 * despawn faster, and the monster cap goes from 70 to {@link #MONSTER_CAP}. Spawners are unaffected.
 */
public final class MobBand {
    private MobBand() {}

    /** Blocks above and below the player that Overworld monster spawn attempts may pick. */
    public static final int BAND = 64;
    /** Monster mob cap (vanilla 70, about 1.25x). */
    public static final int MONSTER_CAP = 88;
    /**
     * Per-tick despawn chance (1 in this) for a despawnable Overworld monster outside every player's band. Vanilla's
     * random despawn is 1 in 800 per tick, and only after 600 idle ticks.
     */
    public static final int FAR_DESPAWN_CHANCE = 40;

    /**
     * The inclusive Y range a spawn attempt may pick, or null when the band holds no valid Y (the attempt is skipped).
     *
     * @param surfaceTop vanilla's top: the WORLD_SURFACE heightmap at the column, plus one
     */
    public static int[] range(int playerY, int bottomY, int surfaceTop) {
        int lo = Math.max(bottomY, playerY - BAND);
        int hi = Math.min(surfaceTop, playerY + BAND);
        return lo > hi ? null : new int[] {lo, hi};
    }

    /** Whether a mob at this Y is inside a player's band. */
    public static boolean inBand(double mobY, double playerY) {
        return Math.abs(mobY - playerY) <= BAND;
    }
}
