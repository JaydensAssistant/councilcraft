package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import java.util.List;
import net.minecraft.text.Text;

/**
 * The Reborn hover is two lines. The first is a fixed sentence. The second is
 * the live size, speed, strength and HP. There is no floor-stats line.
 *
 * <p>Order is size, speed, strength, HP. Strength is the melee multiplier.
 * HP is the stepped hit points over a vanilla 20, so 12 points is 60% HP.
 */
public final class RebornText {
    public static final String LINE1 = "effect.councilcraft.reborn.line1";
    public static final String NOW = "effect.councilcraft.reborn.now";
    public static final String NAME = "effect.councilcraft.reborn";

    private RebornText() {
    }

    public static int percent(float fraction) {
        return Math.round(fraction * 100f);
    }

    /** Stepped hit points as a percent of a vanilla 20-point bar. */
    public static int hpPercent(int hitPoints) {
        return hitPoints * 100 / CouncilCraftRules.VANILLA_HP;
    }

    /** Size, speed, strength, HP at the start of the penalty. Not a hover line. */
    public static int[] startingPercents() {
        return new int[] {
            percent(CouncilCraftRules.REBORN_SCALE),
            percent(CouncilCraftRules.REBORN_SPEED),
            percent(CouncilCraftRules.REBORN_MELEE),
            hpPercent(CouncilCraftRules.REBORN_BASE_HP)
        };
    }

    /** Size, speed, strength, HP for one moment of the penalty. */
    public static int[] nowPercents(RebornStats stats) {
        return new int[] {
            percent(stats.scale),
            percent(stats.speed),
            percent(stats.melee),
            hpPercent(stats.hitPoints)
        };
    }

    public static Text line1() {
        return Text.translatable(LINE1);
    }

    public static Text now(RebornStats stats) {
        int[] percents = nowPercents(stats);
        return Text.translatable(NOW, percents[0], percents[1], percents[2], percents[3]);
    }

    public static List<Text> hover(RebornStats stats) {
        return List.of(line1(), now(stats));
    }

    /** True when this tooltip line is the Reborn effect's own name. */
    public static boolean isRebornName(Text line) {
        return line != null && line.getString().equals(Text.translatable(NAME).getString());
    }
}
