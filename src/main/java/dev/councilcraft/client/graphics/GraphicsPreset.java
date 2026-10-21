package dev.councilcraft.client.graphics;

import org.jetbrains.annotations.Nullable;

/**
 * PPM61. The three graphics presets. Vanilla render, simulation and shadow distance stay 8 for all of them
 * (shadow distance 8 chunks lives in the Complementary settings file, PPM40), VSync is off and the frame rate
 * is unlimited.
 */
public enum GraphicsPreset {
    /** Complementary Unbound r5.9.3 with the pack's settings file; DH High, 256 chunks. */
    HIGH("ComplementaryUnbound_r5.9.3.zip", "HIGH", 256),
    /** E-LITE 5.1.3 with its default settings; DH Medium, 186 chunks. */
    MEDIUM("\u00a7lE-LITE shaders 5.1.3.zip", "MEDIUM", 186),
    /** Shaders off; DH Low, 128 chunks. */
    LOW(null, "LOW", 128);

    public static final int RENDER_DISTANCE = 8;
    public static final int SIMULATION_DISTANCE = 8;
    /** 1.20.1 treats a max frame rate of 260 as "Unlimited". */
    public static final int UNLIMITED_FPS = 260;

    @Nullable
    private final String shaderPack;
    private final String dhQuality;
    private final int dhRenderDistance;

    GraphicsPreset(@Nullable String shaderPack, String dhQuality, int dhRenderDistance) {
        this.shaderPack = shaderPack;
        this.dhQuality = dhQuality;
        this.dhRenderDistance = dhRenderDistance;
    }

    /** The shader pack file name in shaderpacks/, or null for shaders off. */
    @Nullable
    public String shaderPack() {
        return this.shaderPack;
    }

    /** A Distant Horizons EDhApiQualityPreset constant name. */
    public String dhQuality() {
        return this.dhQuality;
    }

    public int dhRenderDistance() {
        return this.dhRenderDistance;
    }

    public String translationKey() {
        return "councilcraft.graphics.preset." + this.name().toLowerCase(java.util.Locale.ROOT);
    }

    @Nullable
    public static GraphicsPreset byName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (GraphicsPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(name.trim())) {
                return preset;
            }
        }
        return null;
    }
}
