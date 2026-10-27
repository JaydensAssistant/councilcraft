package net.blay09.mods.balm.api.event;

import java.util.function.Consumer;

/** Compile-time stand-in for Balm 7.3.44's interface of the same name (PT42). Not packed into our jar. */
public interface BalmEvents {
    default <T> void onEvent(Class<T> eventClass, Consumer<T> handler) {
        throw new UnsupportedOperationException("stub");
    }
}
