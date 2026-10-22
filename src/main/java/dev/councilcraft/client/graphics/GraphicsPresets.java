package dev.councilcraft.client.graphics;

import dev.councilcraft.CouncilCraft;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * PPM61 / PPM50 amended. Graphics presets.
 *
 * <ul>
 *   <li>First launch (no stored choice): the first title screen opens {@link GraphicsPresetScreen}. Closing it without
 *   a pick means High.</li>
 *   <li>A "Graphics Preset" button in Video Settings (vanilla, Sodium or Reese's Sodium Options) re-opens it.</li>
 *   <li>Picking applies the preset once. Manual tweaks afterwards stick. A pack update (a new councilcraft version)
 *   re-applies only the stored choice, never a reset to High, because the update overwrites the forced DH config.</li>
 * </ul>
 * The choice lives in {@code config/councilcraft-graphics.properties}, which the pack does not ship.
 */
public final class GraphicsPresets {
    private static final String FILE = "councilcraft-graphics.properties";
    /** Sodium 0.5 and Reese's Sodium Options replace the vanilla Video Settings screen. */
    private static final String[] VIDEO_SCREENS = {
        "me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI",
        "me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen"
    };
    private static final String RSO_SCREEN = VIDEO_SCREENS[1];
    private static boolean checkedThisSession;

    private GraphicsPresets() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen && !checkedThisSession) {
                checkedThisSession = true;
                onFirstTitleScreen(client, screen);
            }
            if (isVideoSettings(screen)) {
                ButtonWidget presetButton = ButtonWidget.builder(Text.translatable("councilcraft.graphics.button"),
                        button -> client.setScreen(new GraphicsPresetScreen(screen)))
                    .dimensions(6, height - 26, 120, 20)
                    .build();
                if (RSO_SCREEN.equals(screen.getClass().getName())) {
                    drawOutsideWidgets(screen, presetButton);
                } else {
                    Screens.getButtons(screen).add(presetButton);
                }
            }
        });
    }

    /**
     * PPM76. Reese's Sodium Options renders only its own frame, never the screen's widget list, so the button is drawn
     * after the screen and gets its clicks before the screen does. Fabric renews these per-screen events on every
     * init, so a resize or RSO's own rebuild never stacks a second button.
     */
    private static void drawOutsideWidgets(Screen screen, ButtonWidget button) {
        ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, delta) ->
            button.render(context, mouseX, mouseY, delta));
        ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, mouseButton) ->
            !button.mouseClicked(mouseX, mouseY, mouseButton));
    }

    private static void onFirstTitleScreen(MinecraftClient client, Screen title) {
        Properties stored = load();
        GraphicsPreset preset = GraphicsPreset.byName(stored.getProperty("preset"));
        if (preset == null) {
            client.setScreen(new GraphicsPresetScreen(title));
        } else if (!modVersion().equals(stored.getProperty("appliedFor"))) {
            apply(client, preset);
        }
    }

    private static boolean isVideoSettings(Screen screen) {
        if (screen instanceof VideoOptionsScreen) {
            return true;
        }
        String name = screen.getClass().getName();
        for (String videoScreen : VIDEO_SCREENS) {
            if (videoScreen.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Applies the preset now and stores it as the player's choice. */
    public static void apply(MinecraftClient client, GraphicsPreset preset) {
        GameOptions options = client.options;
        options.getViewDistance().setValue(GraphicsPreset.RENDER_DISTANCE);
        options.getSimulationDistance().setValue(GraphicsPreset.SIMULATION_DISTANCE);
        options.getEnableVsync().setValue(false);
        options.getMaxFps().setValue(GraphicsPreset.UNLIMITED_FPS);
        options.write();
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("distanthorizons")) {
            DhPresetApplier.apply(preset);
        }
        if (loader.isModLoaded("iris")) {
            try {
                IrisPresetApplier.apply(preset);
            } catch (IOException e) {
                CouncilCraft.LOGGER.error("CouncilCraft graphics preset: could not write the Iris config", e);
            }
        }
        Properties stored = new Properties();
        stored.setProperty("preset", preset.name());
        stored.setProperty("appliedFor", modVersion());
        save(stored);
        CouncilCraft.LOGGER.info("CouncilCraft graphics preset {} applied", preset);
    }

    @Nullable
    public static GraphicsPreset current() {
        return GraphicsPreset.byName(load().getProperty("preset"));
    }

    private static String modVersion() {
        return CouncilCraft.version();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    private static Properties load() {
        Properties properties = new Properties();
        Path path = file();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                CouncilCraft.LOGGER.warn("CouncilCraft graphics preset: could not read {}", path, e);
            }
        }
        return properties;
    }

    private static void save(Properties properties) {
        try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            properties.store(writer, "CouncilCraft graphics preset (PPM61). Change it in Video Settings > Graphics Preset.");
        } catch (IOException e) {
            CouncilCraft.LOGGER.error("CouncilCraft graphics preset: could not save the choice", e);
        }
    }
}
