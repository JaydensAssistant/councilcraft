package de.maxhenkel.voicechat.api.events;

import java.util.function.Consumer;

/** Compile-time stand-in for Simple Voice Chat 2.6.24 API type of the same name (PT42). Not packed into our jar. */
public interface EventRegistration {
    default <T extends Event> void registerEvent(Class<T> eventClass, Consumer<T> handler) {
        throw new UnsupportedOperationException("stub");
    }
}
