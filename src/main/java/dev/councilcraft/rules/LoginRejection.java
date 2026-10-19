package dev.councilcraft.rules;

/**
 * A login refusal is held until every login query has been answered.
 * Disconnecting from the query callback closes the socket while the client is
 * still answering other mods, and the client reports "Connection reset by peer"
 * instead of the reason.
 */
public final class LoginRejection {
    private String message;

    public void record(String message) {
        if (this.message == null) {
            this.message = message;
        }
    }

    /** The text to send at accept-player, or null when login may continue. */
    public String take() {
        String held = this.message;
        this.message = null;
        return held;
    }
}
