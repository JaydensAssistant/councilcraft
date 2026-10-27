package net.blay09.mods.balm.api;

import net.blay09.mods.balm.api.event.BalmEvents;

/** Compile-time stand-in for Balm 7.3.44's class of the same name (PT42). Not packed into our jar. */
public class Balm {
    public static BalmEvents getEvents() {
        throw new UnsupportedOperationException("stub");
    }
}
