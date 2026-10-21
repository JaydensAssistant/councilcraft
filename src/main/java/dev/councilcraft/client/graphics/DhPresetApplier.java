package dev.councilcraft.client.graphics;

import com.seibel.distanthorizons.api.enums.config.quickOptions.EDhApiQualityPreset;
import com.seibel.distanthorizons.core.config.Config;

/**
 * PPM61. Loaded only when Distant Horizons is present. Sets DH's own quality preset (its listener writes the
 * underlying quality values) and the LOD render distance, through DH's persistent config, so later manual changes
 * in the DH menu stick. The DH API's setValue is an override layer that would lock those options instead.
 */
final class DhPresetApplier {
    private DhPresetApplier() {
    }

    static void apply(GraphicsPreset preset) {
        Config.Client.qualityPresetSetting.set(EDhApiQualityPreset.valueOf(preset.dhQuality()));
        Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistanceRadius.set(preset.dhRenderDistance());
    }
}
