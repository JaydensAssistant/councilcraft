package dev.councilcraft.client.graphics;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * PPM61. "High / Medium / Low" with a plain-language hardware hint under each button. Closing the screen without a
 * pick applies High when no preset has been chosen yet; from Video Settings it just goes back.
 */
public final class GraphicsPresetScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int ROW = 46;
    private final Screen parent;
    private final boolean firstChoice;

    public GraphicsPresetScreen(Screen parent) {
        super(Text.translatable("councilcraft.graphics.title"));
        this.parent = parent;
        this.firstChoice = GraphicsPresets.current() == null;
    }

    @Override
    protected void init() {
        int top = this.height / 2 - 70;
        GraphicsPreset current = GraphicsPresets.current();
        GraphicsPreset[] presets = GraphicsPreset.values();
        for (int i = 0; i < presets.length; i++) {
            GraphicsPreset preset = presets[i];
            Text label = Text.translatable(preset.translationKey());
            if (preset == current) {
                label = Text.translatable("councilcraft.graphics.current", label);
            }
            this.addDrawableChild(ButtonWidget.builder(label, button -> this.choose(preset))
                .dimensions(this.width / 2 - BUTTON_WIDTH / 2, top + i * ROW, BUTTON_WIDTH, 20)
                .build());
        }
    }

    private void choose(GraphicsPreset preset) {
        GraphicsPresets.apply(this.client, preset);
        this.client.setScreen(this.parent);
    }

    @Override
    public void close() {
        if (this.firstChoice && GraphicsPresets.current() == null) {
            this.choose(GraphicsPreset.HIGH);
        } else {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        int top = this.height / 2 - 70;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top - 34, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.translatable("councilcraft.graphics.subtitle").formatted(Formatting.GRAY), this.width / 2, top - 20, 0xFFFFFF);
        GraphicsPreset[] presets = GraphicsPreset.values();
        for (int i = 0; i < presets.length; i++) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable(presets[i].translationKey() + ".hint").formatted(Formatting.GRAY),
                this.width / 2, top + i * ROW + 24, 0xFFFFFF);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
