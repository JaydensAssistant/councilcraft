package dev.councilcraft.print;

import java.util.Locale;

/**
 * PT14 amendment prices, in millibuckets. Pure arithmetic so the rules test can check it.
 * Every photograph costs 200 mB (black dye fluid for black-and-white, process ink for color).
 * An album costs 200 mB per page with a photo plus 10 mB per page with a note.
 * The printer, its goggles, EMI and the copy-cost tooltips (PT25-PT28) all read these.
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

    /** True when one copy's cost fits in the printer's tank (CEI's own isValid rule: 0 < cost <= capacity). */
    public static boolean fitsPrinter(long costMb, long capacityMb) {
        return costMb > 0 && costMb <= capacityMb;
    }

    /** PT29. Whole numbers with thousands separators: 1000000 -> "1,000,000". */
    public static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
