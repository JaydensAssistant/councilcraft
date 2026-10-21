package dev.councilcraft.client.graphics;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.IrisConfig;

/** PPM61. Loaded only when Iris is present. Writes the shader choice to iris.properties and reloads Iris. */
final class IrisPresetApplier {
    private IrisPresetApplier() {
    }

    static void apply(GraphicsPreset preset) throws java.io.IOException {
        IrisConfig config = Iris.getIrisConfig();
        if (preset.shaderPack() != null) {
            config.setShaderPackName(preset.shaderPack());
            config.setShadersEnabled(true);
        } else {
            config.setShadersEnabled(false);
        }
        config.save();
        Iris.reload();
    }
}
