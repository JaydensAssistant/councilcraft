package dev.councilcraft.compat;

import dev.councilcraft.advancement.MilestoneRules;
import dev.councilcraft.advancement.Milestones;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.hardcorerevival.api.PlayerRescuedEvent;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * PT42 Not On My Watch. Hardcore Revival fires PlayerRescuedEvent only when a rescuer finishes reviving a downed
 * player (not for /revive or a timeout). The rescuer gets the advancement. Separate class so Balm types load only
 * with Hardcore Revival.
 */
public final class HardcoreRevivalRescue {
    private HardcoreRevivalRescue() {
    }

    public static void register() {
        Balm.getEvents().onEvent(PlayerRescuedEvent.class, event -> {
            if (event.getRescuer() instanceof ServerPlayerEntity rescuer && event.getRescuer() != event.getPlayer()) {
                Milestones.grant(rescuer, MilestoneRules.NOT_ON_MY_WATCH);
            }
        });
    }
}
