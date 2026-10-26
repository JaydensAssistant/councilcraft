package io.github.mortuusars.exposure.client.gui.screen.album;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Compile-time stand-in for Exposure 1.9.22's class of the same name (Exposure's jar was built with a newer
 * Loom than this project can remap). Only the members councilcraft uses. Not packed into our jar.
 */
public abstract class PhotographSlotWidget extends ClickableWidget {
    protected boolean editable;
    protected boolean hasPhotograph;

    protected PhotographSlotWidget() {
        super(0, 0, 0, 0, Text.empty());
    }

    public boolean isEditable() {
        throw new UnsupportedOperationException("stub");
    }

    public ItemStack getPhotograph() {
        throw new UnsupportedOperationException("stub");
    }
}
