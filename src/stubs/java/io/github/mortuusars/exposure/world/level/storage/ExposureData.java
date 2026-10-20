package io.github.mortuusars.exposure.world.level.storage;

/**
 * Compile-time stand-in for Exposure 1.9.22's class of the same name (Exposure's jar was built with a newer
 * Loom than this project can remap). Only the members councilcraft calls. Not packed into our jar.
 */
public class ExposureData {
    public int getWidth() {
        throw new UnsupportedOperationException("stub");
    }

    public int getHeight() {
        throw new UnsupportedOperationException("stub");
    }

    /** Stand-in for ExposureData.Tag (a record in Exposure). */
    public static final class Tag {
        private Tag() {
        }
    }
}
