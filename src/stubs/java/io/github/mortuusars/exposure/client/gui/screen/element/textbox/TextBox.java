package io.github.mortuusars.exposure.client.gui.screen.element.textbox;

import java.util.function.Predicate;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * Compile-time stand-in for Exposure 1.9.22's class of the same name (Exposure's jar was built with a newer
 * Loom than this project can remap). Only the members councilcraft uses. Not packed into our jar.
 */
public abstract class TextBox extends ClickableWidget {
    public Predicate<String> textValidator;
    public HorizontalAlignment horizontalAlignment;
    public int fontColor;

    private TextBox() {
        super(0, 0, 0, 0, Text.empty());
    }

    public String getText() {
        throw new UnsupportedOperationException("stub");
    }

    /** Exposure's own method (not a Minecraft override in 1.20.1); also clears the display cache. */
    public void setHeight(int height) {
        throw new UnsupportedOperationException("stub");
    }

    public void refresh() {
        throw new UnsupportedOperationException("stub");
    }
}
