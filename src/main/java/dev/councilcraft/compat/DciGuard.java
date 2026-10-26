package dev.councilcraft.compat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** PT18. One clear warning when Discord Integration's bot is missing and its join/leave hooks are skipped. */
public final class DciGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger("councilcraft");
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private DciGuard() {
    }

    public static void warnOnce() {
        if (WARNED.compareAndSet(false, true)) {
            LOGGER.warn("CouncilCraft: Discord Integration's bot is not running (no token, intents off or missing"
                + " permissions). Its join/leave hooks and /discord are skipped until it runs; nothing is sent to Discord.");
        }
    }
}
