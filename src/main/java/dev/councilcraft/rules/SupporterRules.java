package dev.councilcraft.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PPM102. Donor tiers, badge and name colors, role parsing and the tab-list footer order. No Minecraft types,
 * so it is unit tested without a game.
 */
public final class SupporterRules {
    public static final String BADGE = "\u2605";
    /** Donor tiers in dollars, lowest first. */
    public static final List<Integer> TIERS = List.of(2, 5, 10, 15, 20);
    /** $20 donors without a role get a gold name. */
    public static final int GOLD = 0xFFAA00;
    public static final int MAX_PREFIX_LENGTH = 32;

    private static final Pattern ROLE_ID = Pattern.compile("[a-z0-9_]{1,32}");
    private static final Pattern HEX = Pattern.compile("#([0-9A-Fa-f]{6})");
    private static final Pattern ROLE_SPEC = Pattern.compile("(#[0-9A-Fa-f]{6})(?:\\s+\"(.*)\")?\\s*");

    private SupporterRules() {
    }

    public static boolean isTier(int dollars) {
        return TIERS.contains(dollars);
    }

    /** Badge color for a tier: $2 green, $5 aqua, $10 blue, $15 pink, $20 gold. */
    public static int tierColor(int dollars) {
        return switch (dollars) {
            case 2 -> 0x55FF55;
            case 5 -> 0x55FFFF;
            case 10 -> 0x5599FF;
            case 15 -> 0xFF77AA;
            case 20 -> GOLD;
            default -> throw new IllegalArgumentException("Not a donor tier: $" + dollars);
        };
    }

    /**
     * Name color: the role color, else gold for $20 donors, else empty (the name keeps its default color).
     * {@code roleColor} and {@code tier} are null when the player has no role or is not a donor.
     */
    public static OptionalInt nameColor(Integer roleColor, Integer tier) {
        if (roleColor != null) {
            return OptionalInt.of(roleColor);
        }
        if (tier != null && tier == 20) {
            return OptionalInt.of(GOLD);
        }
        return OptionalInt.empty();
    }

    public static boolean isValidRoleId(String id) {
        return id != null && ROLE_ID.matcher(id).matches();
    }

    public static OptionalInt parseHexColor(String text) {
        Matcher m = HEX.matcher(text == null ? "" : text.trim());
        return m.matches() ? OptionalInt.of(Integer.parseInt(m.group(1), 16)) : OptionalInt.empty();
    }

    public static String hex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    /** A role's color and its prefix ("" for none). */
    public record RoleSpec(int color, String prefix) {
    }

    /**
     * Parses the rest of {@code /ccrole create <id> ...}: {@code #RRGGBB} then an optional prefix in double
     * quotes, e.g. {@code #9B30FF "[President]"}. Throws IllegalArgumentException with a message for the player.
     */
    public static RoleSpec parseRoleSpec(String text) {
        Matcher m = ROLE_SPEC.matcher(text == null ? "" : text.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Expected #RRGGBB then an optional \"prefix\" in double quotes");
        }
        String prefix = m.group(2) == null ? "" : m.group(2).trim();
        if (prefix.length() > MAX_PREFIX_LENGTH) {
            throw new IllegalArgumentException("Prefix is longer than " + MAX_PREFIX_LENGTH + " characters");
        }
        if (prefix.indexOf('\u00A7') >= 0) {
            throw new IllegalArgumentException("Prefix cannot contain \u00A7 formatting codes; the role color is used");
        }
        return new RoleSpec(Integer.parseInt(m.group(1).substring(1), 16), prefix);
    }

    /**
     * Tab-list footer lines: one per tier from highest to lowest, names sorted case-insensitively, empty tiers left
     * out. Keys are tiers in display order.
     */
    public static Map<Integer, List<String>> footerLines(Map<String, Integer> donorTiers) {
        Map<Integer, List<String>> lines = new LinkedHashMap<>();
        List<Integer> tiers = new ArrayList<>(TIERS);
        Collections.reverse(tiers);
        for (int tier : tiers) {
            List<String> names = new ArrayList<>();
            donorTiers.forEach((name, t) -> {
                if (t != null && t == tier) {
                    names.add(name);
                }
            });
            if (!names.isEmpty()) {
                names.sort(Comparator.comparing((String s) -> s.toLowerCase(java.util.Locale.ROOT)).thenComparing(s -> s));
                lines.put(tier, names);
            }
        }
        return lines;
    }
}
