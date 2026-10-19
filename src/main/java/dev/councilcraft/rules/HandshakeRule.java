package dev.councilcraft.rules;

/**
 * S244 G5. The joining client must be running the same CouncilCraft version as the server.
 * A client without the mod never loads our language file, so both refusals are literal text.
 */
public final class HandshakeRule {
    /** Shown when the login query is not understood. The client has no CouncilCraft channel. */
    public static final String MISSING_MOD =
        "This server requires the CouncilCraft mod. Install the CouncilCraft modpack.";

    private HandshakeRule() {
    }

    public static boolean versionsMatch(String serverVersion, String clientVersion) {
        return serverVersion != null && serverVersion.equals(clientVersion);
    }

    public static String versionMismatch(String serverVersion, String clientVersion) {
        return "CouncilCraft version mismatch. Server has " + serverVersion + ", client has " + clientVersion + ".";
    }
}
