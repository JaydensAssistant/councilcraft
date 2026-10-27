package de.maxhenkel.voicechat.api;

/** Compile-time stand-in for Simple Voice Chat 2.6.24 API type of the same name (PT42). Not packed into our jar. */
public interface VoicechatConnection {
    boolean isInGroup();

    ServerPlayer getPlayer();
}
