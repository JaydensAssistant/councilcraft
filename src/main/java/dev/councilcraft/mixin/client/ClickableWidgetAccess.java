package dev.councilcraft.mixin.client;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** PT14. Exposure's TextBlock sizes its lines from the widget height, which 1.20.1 has no setter for. */
@Mixin(ClickableWidget.class)
public interface ClickableWidgetAccess {
    @Accessor("height")
    void councilcraft$setHeight(int height);
}
