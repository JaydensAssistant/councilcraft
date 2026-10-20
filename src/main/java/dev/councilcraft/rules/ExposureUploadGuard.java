package dev.councilcraft.rules;

/**
 * PM26 (3). Exposure only accepts uploads the server asked for, but it trusts the client's image size:
 * a chunked-upload header allocates width x height bytes before any pixels arrive. Every image Exposure
 * makes is square (the capture template crops to a square) and its frame size is limited to 1..2048 by
 * Exposure's own config and film codecs, so anything else is rejected.
 */
public final class ExposureUploadGuard {
    /** Exposure's frame-size ceiling (config default_frame_size and the film frame_size codec). */
    public static final int MAX_SIDE = 2048;

    private ExposureUploadGuard() {
    }

    public static boolean acceptable(int width, int height) {
        return width > 0 && width <= MAX_SIDE && width == height;
    }
}
