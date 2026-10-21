package dev.councilcraft.rules;

import java.util.Locale;

/**
 * PPM8. One money total for a Numismatics amount (one unit is one cent here): under 100 it is "N¢",
 * otherwise "$D.CC" with digit grouping. Pure arithmetic, no Minecraft classes.
 */
public final class CoinMoney {
    public static final int CENTS_PER_DOLLAR = 100;

    private CoinMoney() {
    }

    public static String format(long total) {
        String sign = total < 0 ? "-" : "";
        long abs = Math.abs(total);
        if (abs < CENTS_PER_DOLLAR) {
            return sign + abs + "\u00a2";
        }
        return String.format(Locale.ROOT, "%s$%,d.%02d", sign, abs / CENTS_PER_DOLLAR, abs % CENTS_PER_DOLLAR);
    }
}
