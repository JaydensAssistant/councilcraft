package de.maxhenkel.voicechat.api;

import de.maxhenkel.voicechat.api.events.EventRegistration;

/** Compile-time stand-in for Simple Voice Chat 2.6.24 API type of the same name (PT42). Not packed into our jar. */
public interface VoicechatPlugin {
    String getPluginId();

    default void registerEvents(EventRegistration registration) {
    }
}
