package dev.councilcraft.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PPM102. Donor tiers, badge and name colors, role parsing and the tab-list footer order. No Minecraft types,
 * so it is unit tested without a game. PPM107 (0.4.13): material badge ladder and a soft blue default name.
 * PPM108 (0.4.13): {@code /ccrole create <Name> <color> ["<prefix>"]}.
 */
public final class SupporterRules {
    public static final String BADGE = "\u2605";
    /** Donor tiers in dollars, lowest first. */
    public static final List<Integer> TIERS = List.of(2, 5, 10, 15, 20);
    /** $20 donors without a role get a gold name (not bold). */
    public static final int GOLD = 0xFFAA00;
    /** PPM107. Everyone else without a role: soft blue. */
    public static final int DEFAULT_NAME = 0x7FB2FF;
    public static final int MAX_PREFIX_LENGTH = 32;

    private static final Pattern ROLE_ID = Pattern.compile("[a-z0-9_]{1,32}");
    private static final Pattern HEX = Pattern.compile("#([0-9A-Fa-f]{6})");
    private static final Pattern ROLE_SPEC = Pattern.compile("(\\S+)(?:\\s+\"(.*)\")?\\s*");

    /**
     * PPM108. Role colors by name: Minecraft's 16 named colors (their chat RGB values), then the extras. Names are
     * matched case-insensitively; tab completion offers these keys.
     */
    public static final Map<String, Integer> NAMED_COLORS = namedColors();

    private static Map<String, Integer> namedColors() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("black", 0x000000);
        m.put("dark_blue", 0x0000AA);
        m.put("dark_green", 0x00AA00);
        m.put("dark_aqua", 0x00AAAA);
        m.put("dark_red", 0xAA0000);
        m.put("dark_purple", 0xAA00AA);
        m.put("gold", 0xFFAA00);
        m.put("gray", 0xAAAAAA);
        m.put("dark_gray", 0x555555);
        m.put("blue", 0x5555FF);
        m.put("green", 0x55FF55);
        m.put("aqua", 0x55FFFF);
        m.put("red", 0xFF5555);
        m.put("light_purple", 0xFF55FF);
        m.put("yellow", 0xFFFF55);
        m.put("white", 0xFFFFFF);
        m.put("purple", 0x9B30FF);
        m.put("royal_purple", 0x9B30FF);
        return Collections.unmodifiableMap(m);
    }

    private SupporterRules() {
    }

    public static boolean isTier(int dollars) {
        return TIERS.contains(dollars);
    }

    /** PPM107. Badge color for a tier: $2 copper, $5 iron, $10 emerald, $15 diamond, $20 gold. */
    public static int tierColor(int dollars) {
        return switch (dollars) {
            case 2 -> 0xD9814C;
            case 5 -> 0xD8D8D8;
            case 10 -> 0x3DDC6E;
            case 15 -> 0x4DE8E0;
            case 20 -> GOLD;
            default -> throw new IllegalArgumentException("Not a donor tier: $" + dollars);
        };
    }

    /** PPM107. The $15 and $20 stars (badge and footer label) are bold. */
    public static boolean boldBadge(int dollars) {
        return dollars >= 15;
    }

    /**
     * PPM107. Name color: the role color, else gold for $20 donors, else the default soft blue. {@code roleColor}
     * and {@code tier} are null when the player has no role or is not a donor.
     */
    public static int nameColor(Integer roleColor, Integer tier) {
        if (roleColor != null) {
            return roleColor;
        }
        if (tier != null && tier == 20) {
            return GOLD;
        }
        return DEFAULT_NAME;
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

    /** PPM108. A named color (case-insensitive, {@link #NAMED_COLORS}) or #RRGGBB. */
    public static OptionalInt parseColor(String text) {
        String t = text == null ? "" : text.trim();
        Integer named = NAMED_COLORS.get(t.toLowerCase(Locale.ROOT));
        return named != null ? OptionalInt.of(named) : parseHexColor(t);
    }

    /** PPM108. The role id for a role Name: its lowercase form. Valid only if {@link #isValidRoleId} accepts it. */
    public static String roleId(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    /** A role's id, color and prefix ("" for none). */
    public record RoleSpec(String id, int color, String prefix) {
    }

    /**
     * PPM108. Parses {@code /ccrole create <Name> <color> ["<prefix>"]}: {@code name} is the Name argument and
     * {@code rest} is the color (a named color or #RRGGBB) then an optional prefix in double quotes. The id is the
     * lowercase Name and the prefix defaults to "[Name]"; a quoted prefix overrides it ({@code ""} for none).
     * Throws IllegalArgumentException with a message for the player.
     */
    public static RoleSpec parseRoleSpec(String name, String rest) {
        String id = roleId(name);
        if (!isValidRoleId(id)) {
            throw new IllegalArgumentException("Role name must be 1-32 characters of A-Z, a-z, 0-9 and _");
        }
        Matcher m = ROLE_SPEC.matcher(rest == null ? "" : rest.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Expected a color (e.g. gold, purple or #RRGGBB) then an optional \"prefix\" in double quotes");
        }
        OptionalInt color = parseColor(m.group(1));
        if (color.isEmpty()) {
            throw new IllegalArgumentException("Unknown color " + m.group(1) + ": use a color name (e.g. gold, purple) or #RRGGBB");
        }
        if (m.group(2) == null) {
            return new RoleSpec(id, color.getAsInt(), "[" + name.trim() + "]");
        }
        String prefix = m.group(2).trim();
        if (prefix.length() > MAX_PREFIX_LENGTH) {
            throw new IllegalArgumentException("Prefix is longer than " + MAX_PREFIX_LENGTH + " characters");
        }
        if (prefix.indexOf('\u00A7') >= 0) {
            throw new IllegalArgumentException("Prefix cannot contain \u00A7 formatting codes; the role color is used");
        }
        return new RoleSpec(id, color.getAsInt(), prefix);
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
