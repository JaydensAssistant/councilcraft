package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import java.util.List;
import net.minecraft.text.Text;

/**
 * The Reborn hover is two lines. The first is a fixed sentence. The second is
 * the live size, speed, strength, HP and hunger (PPM67 added hunger there). PPM97
 * removed PPM67's separate explanatory hunger line; the short "150% hunger" in the
 * Now line is the only hunger text, like the other stats.
 *
 * <p>Order is size, speed, strength, HP, hunger. Strength is the melee multiplier.
 * HP is the stepped hit points over a vanilla 20, so 12 points is 60% HP. Hunger is
 * the exhaustion multiplier, so 150% means food drains half again as fast.
 */
public final class RebornText {
    public static final String LINE1 = "effect.councilcraft.reborn.line1";
    public static final String NOW = "effect.councilcraft.reborn.now";
    public static final String NAME = "effect.councilcraft.reborn";
    public static final String SPURT_NAME = "effect.councilcraft.growth_spurt";
    public static final String SPURT_DESC = "effect.councilcraft.growth_spurt.desc";

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

    /** PPM67. The live hunger multiplier as a percent (150 at the start, 100 when grown). */
    public static int hungerPercent(RebornStats stats) {
        return percent(stats.hunger);
    }

    /** PPM67. Hunger at the start of the penalty (150). */
    public static int startingHungerPercent() {
        return percent(CouncilCraftRules.REBORN_HUNGER);
    }

    public static Text line1() {
        return Text.translatable(LINE1);
    }

    public static Text now(RebornStats stats) {
        int[] percents = nowPercents(stats);
        return Text.translatable(NOW, percents[0], percents[1], percents[2], percents[3], hungerPercent(stats));
    }

    public static List<Text> hover(RebornStats stats) {
        return List.of(line1(), now(stats));
    }

    /** True when this tooltip line is the Growth Spurt effect's own name. */
    public static boolean isGrowthSpurtName(Text line) {
        return line != null && line.getString().equals(Text.translatable(SPURT_NAME).getString());
    }

    /** PPM67. The Growth Spurt description line, appended under its name and time. */
    public static Text growthSpurtDescription() {
        return Text.translatable(SPURT_DESC, dev.councilcraft.rules.FeedingRules.RECOVERY_MULTIPLIER);
    }

    /**
     * PPM67. The Growth Spurt tooltip with its description appended once. Vanilla's list passes through
     * two hooks, so a list that already ends in the description is returned as it is.
     */
    public static List<Text> withGrowthSpurtDescription(List<Text> existing) {
        String desc = growthSpurtDescription().getString();
        if (existing.get(existing.size() - 1).getString().equals(desc)) {
            return existing;
        }
        List<Text> lines = new java.util.ArrayList<>(existing);
        lines.add(growthSpurtDescription());
        return lines;
    }

    /** True when this tooltip line is the Reborn effect's own name. */
    public static boolean isRebornName(Text line) {
        return line != null && line.getString().equals(Text.translatable(NAME).getString());
    }
}
