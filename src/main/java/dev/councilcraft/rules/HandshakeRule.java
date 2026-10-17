package dev.councilcraft.rules;

/** S244 G5. The joining client must be running the same CouncilCraft version as the server. */
public final class HandshakeRule {
    private HandshakeRule() {
    }

    public static boolean versionsMatch(String serverVersion, String clientVersion) {
        return serverVersion != null && serverVersion.equals(clientVersion);
    }
}
