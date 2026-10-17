package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import net.minecraft.text.Text;

/**
 * S80. The two description lines and the live "Now" line.
 * Percents come from {@link CouncilCraftRules} and {@link RebornStats}.
 * The language file only has the sentence around them.
 *
 * <p>Order is size, speed, strength, HP. Strength is the melee multiplier.
 * HP is hearts divided by the vanilla ten, so six hearts is 60% HP.
 */
public final class RebornText {
    public static final String LINE1 = "effect.councilcraft.reborn.line1";
    public static final String LINE2 = "effect.councilcraft.reborn.line2";
    public static final String NOW = "effect.councilcraft.reborn.now";

    private RebornText() {
    }

    public static int percent(float fraction) {
        return Math.round(fraction * 100f);
    }

    /** Hearts as a percent of a vanilla ten-heart bar. */
    public static int hpPercent(int hearts) {
        return hearts * 100 / CouncilCraftRules.VANILLA_HEARTS;
    }

    /** Size, speed, strength, HP at the start of the penalty. */
    public static int[] startingPercents() {
        return new int[] {
            percent(CouncilCraftRules.REBORN_SCALE),
            percent(CouncilCraftRules.REBORN_SPEED),
            percent(CouncilCraftRules.REBORN_MELEE),
            hpPercent(CouncilCraftRules.REBORN_HEARTS)
        };
    }

    /** Size, speed, strength, HP for one moment of the penalty. */
    public static int[] nowPercents(RebornStats stats) {
        return new int[] {
            percent(stats.scale),
            percent(stats.speed),
            percent(stats.melee),
            hpPercent(stats.hearts)
        };
    }

    public static Text line1() {
        return Text.translatable(LINE1);
    }

    public static Text line2() {
        return line(LINE2, startingPercents());
    }

    public static Text now(RebornStats stats) {
        return line(NOW, nowPercents(stats));
    }

    private static Text line(String key, int[] percents) {
        return Text.translatable(key, percents[0], percents[1], percents[2], percents[3]);
    }
}
