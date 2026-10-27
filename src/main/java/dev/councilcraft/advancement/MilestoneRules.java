package dev.councilcraft.advancement;

import dev.councilcraft.CouncilCraftRules;
import java.util.List;

/** PT42. Pure rules for the code-granted CouncilCraft advancements, so they can be unit tested. */
public final class MilestoneRules {
    public static final String NOT_ON_MY_WATCH = "not_on_my_watch";
    public static final String HERE_COMES_THE_AIRPLANE = "here_comes_the_airplane";
    public static final String KEEP_THE_CHANGE = "keep_the_change";
    public static final String HERE_BE_MUTANTS = "here_be_mutants";
    public static final String SHARED_HORIZONS = "shared_horizons";
    public static final String DOUBLE_SADDLE = "double_saddle";
    public static final String TESTING_TESTING = "testing_testing";

    /** Every milestone the councilcraft:milestone criterion accepts. Each has an advancement in councilcraft-data. */
    public static final List<String> ALL = List.of(
        NOT_ON_MY_WATCH, HERE_COMES_THE_AIRPLANE, KEEP_THE_CHANGE, HERE_BE_MUTANTS,
        SHARED_HORIZONS, DOUBLE_SADDLE, TESTING_TESTING
    );

    /** Server ticks between the position, riding and atlas-sharing polls (1 second). */
    public static final int POLL_TICKS = 20;

    private MilestoneRules() {
    }

    /** Same distance metric as MutantSpawnRamp: horizontal distance from the world origin. */
    public static boolean pastMutantLine(double x, double z) {
        return Math.sqrt(x * x + z * z) >= CouncilCraftRules.MUTANT_FREE_RADIUS;
    }

    /** Double Saddle: two or more players on the same horse-family mount. */
    public static boolean doubleSaddle(int playerPassengers) {
        return playerPassengers >= 2;
    }

    /** Surveyor share group: the player's group holds someone besides themself. */
    public static boolean sharing(int groupSize) {
        return groupSize > 1;
    }

    /** Keep the Change: the vendor handed back at least one coin. */
    public static boolean gotChange(int[] change) {
        if (change == null) {
            return false;
        }
        for (int count : change) {
            if (count > 0) {
                return true;
            }
        }
        return false;
    }
}
