package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;
import java.util.ArrayList;
import java.util.List;

/**
 * PM32 / PM32a / PM32b. Numismatics prints a price as "count of the reference coin, name, leftover spurs"
 * ("3 cogs, 0¤"). The pack shows a price as coins instead, largest first ("3 Diamond Quarters, 2 Diamond Dimes").
 * Pure arithmetic, no Minecraft classes.
 */
public final class CoinBreakdown {
    /** Enum name and pack value, largest first. */
    public static final String[] NAMES = {"sun", "crown", "cog", "sprocket", "bevel", "spur"};
    public static final int[] VALUES = {
        CouncilCraftRules.SUN_VALUE, CouncilCraftRules.CROWN_VALUE, CouncilCraftRules.COG_VALUE,
        CouncilCraftRules.SPROCKET_VALUE, CouncilCraftRules.BEVEL_VALUE, CouncilCraftRules.SPUR_VALUE
    };

    private CoinBreakdown() {
    }

    /** One coin type and how many of it. */
    public record Part(String coin, int count) {
    }

    /** Largest coins first; empty for zero or less. */
    public static List<Part> split(int total) {
        List<Part> parts = new ArrayList<>();
        int left = Math.max(0, total);
        for (int i = 0; i < VALUES.length; i++) {
            int n = left / VALUES[i];
            if (n > 0) {
                parts.add(new Part(NAMES[i], n));
                left -= n * VALUES[i];
            }
        }
        return parts;
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
