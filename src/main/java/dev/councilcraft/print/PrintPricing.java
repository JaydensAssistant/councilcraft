package dev.councilcraft.print;

/**
 * PT14 amendment prices, in millibuckets. Pure arithmetic so the rules test can check it.
 * Every photograph costs 200 mB (black dye fluid for black-and-white, process ink for color).
 * An album costs 200 mB per page with a photo plus 10 mB per page with a note.
 */
public final class PrintPricing {
    public static final int PHOTO_MB = 200;
    public static final int NOTE_MB = 10;
    /** Exposure albums hold 16 pages. */
    public static final int ALBUM_PAGES = 16;

    private PrintPricing() {
    }

    public static int albumCost(int photoPages, int notePages) {
        return photoPages * PHOTO_MB + notePages * NOTE_MB;
    }
}
