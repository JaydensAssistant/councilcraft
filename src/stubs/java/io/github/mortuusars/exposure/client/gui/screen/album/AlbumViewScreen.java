package io.github.mortuusars.exposure.client.gui.screen.album;

import io.github.mortuusars.exposure.client.gui.screen.element.TextBlock;

/**
 * Compile-time stand-in for Exposure 1.9.22's class of the same name (Exposure's jar was built with a newer
 * Loom than this project can remap). Only the members councilcraft uses. Not packed into our jar.
 */
public abstract class AlbumViewScreen {
    private AlbumViewScreen() {
    }

    /** Stand-in for AlbumViewScreen.Page (a protected record in Exposure). */
    public static final class Page {
        public PhotographSlotWidget photographWidget() {
            throw new UnsupportedOperationException("stub");
        }

        public TextBlock noteWidget() {
            throw new UnsupportedOperationException("stub");
        }
    }
}
