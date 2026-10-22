package dev.councilcraft.client.compat;

import com.connectedpaths.core.model.Configs;
import dev.councilcraft.rules.ConnectedPathsRules;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PPM67. Applies {@link ConnectedPathsRules} to Connected Paths' loaded configuration, and keeps the
 * marker that forces one regeneration for installs whose packs were built under the stock rules.
 * Called only from the Connected Paths mixins, so its types load only with that mod.
 */
@Environment(EnvType.CLIENT)
public final class ConnectedPathsTweaks {
    private static final Logger LOGGER = LoggerFactory.getLogger("councilcraft");
    /** Block ids of the stripped groups, as last loaded. Read by the properties rewrite. */
    private static volatile Set<String> strippedBlocks = Collections.emptySet();

    private ConnectedPathsTweaks() {
    }

    public static Set<String> strippedBlocks() {
        return strippedBlocks;
    }

    public static void apply(Configs configs) {
        if (configs == null) {
            return;
        }
        Set<String> blocks = new HashSet<>(strippedBlocks);
        if (configs.groupsDef != null) {
            for (Map.Entry<String, Configs.TomlGroup> entry : configs.groupsDef.entrySet()) {
                if (ConnectedPathsRules.stripped(entry.getKey()) && entry.getValue() != null && entry.getValue().items != null) {
                    blocks.addAll(entry.getValue().items);
                    // The group stays, empty: no overlay is generated for its blocks and none matches them.
                    entry.getValue().items = new ArrayList<>();
                }
            }
        }
        if (configs.groupRelations != null) {
            configs.groupRelations.keySet().removeIf(ConnectedPathsRules::stripped);
            for (Configs.TomlGroup related : configs.groupRelations.values()) {
                if (related != null && related.items != null) {
                    related.items = ConnectedPathsRules.withoutStripped(related.items);
                }
            }
        }
        if (configs.staticAssets != null) {
            Configs.StaticAssetModule grassFix = configs.staticAssets.get(ConnectedPathsRules.GRASS_FIX);
            if (grassFix != null) {
                grassFix.enabled = false;
            }
        }
        strippedBlocks = Collections.unmodifiableSet(blocks);
        LOGGER.info("CouncilCraft: Connected Paths rules applied ({} log/wood/obsidian blocks out of melding, GrassFix off, snow top/bottom only).", blocks.size());
    }

    public static boolean markerCurrent(File baseDir) {
        File marker = new File(baseDir, ConnectedPathsRules.MARKER_FILE);
        try {
            return marker.isFile() && ConnectedPathsRules.markerCurrent(Files.readString(marker.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return false;
        }
    }

    public static void writeMarker(File baseDir) {
        try {
            Files.writeString(new File(baseDir, ConnectedPathsRules.MARKER_FILE).toPath(), ConnectedPathsRules.VERSION + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("CouncilCraft: could not write the Connected Paths rules marker", e);
        }
    }
}
