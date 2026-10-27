package net.blay09.mods.hardcorerevival.api;

import net.blay09.mods.balm.api.event.BalmEvent;
import net.minecraft.entity.player.PlayerEntity;

/** Compile-time stand-in for Hardcore Revival 20.1.19's class of the same name (PT42). Not packed into our jar. */
public class PlayerRescuedEvent extends BalmEvent {
    public PlayerEntity getPlayer() {
        throw new UnsupportedOperationException("stub");
    }

    public PlayerEntity getRescuer() {
        throw new UnsupportedOperationException("stub");
    }
}
