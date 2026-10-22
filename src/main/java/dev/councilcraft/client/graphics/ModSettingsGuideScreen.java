package dev.councilcraft.client.graphics;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/** PPM78. A short guide to where each mod keeps its own settings. */
public final class ModSettingsGuideScreen extends Screen {
    static final int LINES = 7;
    private static final int WIDTH = 320;
    private final Screen parent;

    public ModSettingsGuideScreen(Screen parent) {
        super(Text.translatable("councilcraft.guide.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
            .dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
            .build());
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        int width = Math.min(WIDTH, this.width - 20);
        int x = this.width / 2 - width / 2;
        int y = 30;
        for (int i = 1; i <= LINES; i++) {
            for (OrderedText line : this.textRenderer.wrapLines(Text.translatable("councilcraft.guide.line" + i), width)) {
                context.drawTextWithShadow(this.textRenderer, line, x, y, 0xFFFFFF);
                y += 10;
            }
            y += 4;
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
