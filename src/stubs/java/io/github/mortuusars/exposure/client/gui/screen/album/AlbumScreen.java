package io.github.mortuusars.exposure.client.gui.screen.album;

import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.client.gui.screen.element.TextBlock;
import io.github.mortuusars.exposure.client.gui.screen.element.textbox.TextBox;
import net.minecraft.client.util.math.Rect2i;

/**
 * Compile-time stand-in for Exposure 1.9.22's class of the same name (Exposure's jar was built with a newer
 * Loom than this project can remap). Only the members councilcraft uses. Not packed into our jar.
 */
public abstract class AlbumScreen {
    private AlbumScreen() {
    }

    /** Stand-in for AlbumScreen.Page (a protected inner class in Exposure; its fields are public final). */
    public class Page {
        public final Rect2i pageArea = null;
        public final Rect2i photoArea = null;
        public final Rect2i noteArea = null;
        public final PhotographSlotWidget photographWidget = null;
        public final Either<TextBox, TextBlock> noteWidget = null;
    }
}
