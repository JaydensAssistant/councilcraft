package io.github.mortuusars.exposure.client.gui.screen.element;

import io.github.mortuusars.exposure.client.gui.screen.element.textbox.HorizontalAlignment;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * Compile-time stand-in for Exposure 1.9.22's class of the same name (Exposure's jar was built with a newer
 * Loom than this project can remap). Only the members councilcraft uses. Not packed into our jar.
 */
public abstract class TextBlock extends ClickableWidget {
    public HorizontalAlignment alignment;

    private TextBlock() {
        super(0, 0, 0, 0, Text.empty());
    }
}
