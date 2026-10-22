package dev.councilcraft.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PPM67. What CouncilCraft changes in Connected Paths 1.0.2's generated packs. Pure text, testable.
 *
 * <ul>
 *   <li>Logs, stripped logs, woods, stripped woods and obsidians do not meld: their groups are emptied,
 *   so their own overlays are not generated, and they are dropped from every other overlay's
 *   {@code matchBlocks}.</li>
 *   <li>Snow-family overlays (snow, powder snow and the snowy grass/podzol/mycelium templates) apply to
 *   top and bottom faces only, so the snow side texture does not z-fight with LambdaBetterGrass.</li>
 *   <li>The GrassFix static asset (its own grass/podzol/mycelium blockstates) is off; LambdaBetterGrass
 *   owns those.</li>
 * </ul>
 */
public final class ConnectedPathsRules {
    /** Bump when the rules change, so existing installs regenerate once more. */
    public static final String VERSION = "councilcraft-0.4.7-1";
    public static final String MARKER_FILE = "councilcraft-rules.txt";
    public static final List<String> STRIPPED_GROUPS = List.of("logs", "stripped_logs", "woods", "stripped_woods", "obsidians");
    public static final String GRASS_FIX = "GrassFix";
    public static final String SNOW_FACES = "faces=top bottom";

    private ConnectedPathsRules() {
    }

    public static boolean isSnowFamily(String tileName) {
        return tileName != null && tileName.contains("snow");
    }

    /**
     * Rewrites one generated Continuity overlay .properties text: stripped blocks leave the
     * {@code matchBlocks} line (if nothing is left the overlay matches only its own block, as Connected
     * Paths itself falls back), and a snow-family overlay gets {@link #SNOW_FACES}.
     */
    public static String rewrite(String properties, Collection<String> strippedBlocks, String ownBlock, String tileName) {
        StringBuilder out = new StringBuilder();
        boolean hasFaces = false;
        for (String line : properties.split("\n", -1)) {
            if (line.startsWith("matchBlocks=")) {
                List<String> kept = new ArrayList<>();
                for (String block : line.substring("matchBlocks=".length()).trim().split(" +")) {
                    if (!block.isEmpty() && !strippedBlocks.contains(block)) {
                        kept.add(block);
                    }
                }
                if (kept.isEmpty() && ownBlock != null) {
                    kept.add(ownBlock);
                }
                line = "matchBlocks=" + String.join(" ", kept);
            } else if (line.startsWith("faces=")) {
                hasFaces = true;
                if (isSnowFamily(tileName)) {
                    line = SNOW_FACES;
                }
            }
            out.append(line).append('\n');
        }
        // split(-1) keeps the trailing empty piece; drop the extra newline it adds back.
        out.setLength(out.length() - 1);
        if (isSnowFamily(tileName) && !hasFaces) {
            if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                out.append('\n');
            }
            out.append(SNOW_FACES).append('\n');
        }
        return out.toString();
    }

    /** True when a stored marker means the packs were generated under the current rules. */
    public static boolean markerCurrent(String marker) {
        return marker != null && marker.trim().equals(VERSION);
    }

    /** Whether a group name is one CouncilCraft strips. */
    public static boolean stripped(String group) {
        return STRIPPED_GROUPS.contains(group);
    }

    /** Relation lists without the stripped group names. */
    public static List<String> withoutStripped(List<String> groups) {
        List<String> kept = new ArrayList<>();
        for (String g : groups) {
            if (!stripped(g)) {
                kept.add(g);
            }
        }
        return kept;
    }
}
