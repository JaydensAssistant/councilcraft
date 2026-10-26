package io.github.mortuusars.exposure.world.inventory;

/**
 * Compile-time stand-in for Exposure 1.9.22's class of the same name (Exposure's jar was built with a newer
 * Loom than this project can remap). Only the members councilcraft uses. Not packed into our jar.
 */
public abstract class AlbumMenu {
    private AlbumMenu() {
    }

    public int getCurrentSpreadIndex() {
        throw new UnsupportedOperationException("stub");
    }
}
