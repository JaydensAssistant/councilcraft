package dev.councilcraft.rules;

/**
 * PM32 / PM32a / PM32b. Numismatics prints a price as "count of the reference coin, name, leftover spurs"
 * ("3 cogs, 0¤"). This turns that pair back into one total; PPM8's {@link CoinMoney} formats it.
 * Pure arithmetic, no Minecraft classes.
 */
public final class CoinBreakdown {
    private CoinBreakdown() {
    }

    /**
     * Numismatics passes the reference-coin count already formatted ({@code TextUtils.formatInt},
     * with locale digit grouping). Only the digits and a leading minus carry the number.
     */
    public static int parseCount(Object formatted) {
        if (formatted instanceof Number number) {
            return number.intValue();
        }
        String s = String.valueOf(formatted);
        StringBuilder digits = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        long value = Long.parseLong(digits.toString());
        if (s.trim().startsWith("-")) {
            value = -value;
        }
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    /** Total Dia from Numismatics' (reference count, leftover spurs) pair. */
    public static int total(Object formattedReferenceCount, int referenceValue, Object spurs) {
        return parseCount(formattedReferenceCount) * referenceValue + parseCount(spurs);
    }
}
