package dev.councilcraft.compat;

import dev.councilcraft.advancement.MilestoneRules;
import dev.councilcraft.advancement.Milestones;
import folk.sisby.surveyor.ServerSummary;
import folk.sisby.surveyor.Surveyor;

/**
 * PT42 Shared Horizons. Granted once the player is in a Surveyor share group with someone else
 * ({@code /surveyor share}, accepted by both). Separate class so Surveyor types load only with Surveyor.
 */
public final class SurveyorSharing {
    private SurveyorSharing() {
    }

    public static void register() {
        Milestones.addPoll(player -> {
            ServerSummary summary = ServerSummary.of(player.getServer());
            if (summary != null && MilestoneRules.sharing(summary.groupSize(Surveyor.getUuid(player)))) {
                Milestones.grant(player, MilestoneRules.SHARED_HORIZONS);
            }
        });
    }
}
