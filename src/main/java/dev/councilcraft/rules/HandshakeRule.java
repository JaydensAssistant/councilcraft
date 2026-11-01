package dev.councilcraft.rules;

import java.util.List;

/**
 * S244 G5 / CC-125. The joining client must run a CouncilCraft version the server accepts.
 * A client without the mod never loads our language file, so both refusals are literal text.
 */
public final class HandshakeRule {
    /** Shown when the login query is not understood. The client has no CouncilCraft channel. */
    public static final String MISSING_MOD =
        "This server requires the CouncilCraft mod. Install the CouncilCraft modpack.";

    /**
     * Client versions that may join this server jar. Includes the prior release so players are
     * not forced to update the client when only the server jar moves (CC-125).
     */
    public static final List<String> COMPATIBLE_CLIENT_VERSIONS = List.of("0.4.26", "0.4.27");

    private HandshakeRule() {
    }

    public static boolean versionsMatch(String serverVersion, String clientVersion) {
        return clientVersion != null && COMPATIBLE_CLIENT_VERSIONS.contains(clientVersion);
    }

    public static String versionMismatch(String serverVersion, String clientVersion) {
        return "CouncilCraft version mismatch. Server has " + serverVersion + ", client has " + clientVersion + ".";
    }
}
