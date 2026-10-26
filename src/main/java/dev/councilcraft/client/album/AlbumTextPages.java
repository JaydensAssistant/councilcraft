package dev.councilcraft.client.album;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * PT14. Text-only pages in Exposure's album screens (layout numbers match Exposure 1.9.22's AlbumScreen).
 * A page with no photograph whose note needs a 4th line becomes a full text page: the empty photo frame fades
 * out over FADE_MS and the note moves to the page body. The data format is unchanged; only the screens differ.
 */
public final class AlbumTextPages {
    /** Frame fade time, milliseconds. */
    public static final float FADE_MS = 250.0F;
    /** Note box on a page with a photograph (Exposure's own layout). */
    public static final int STOCK_NOTE_Y = 133;
    public static final int STOCK_NOTE_HEIGHT = 27;
    /** Note box on a full text page: x+22, y+21, 114 x 126 (14 lines, the book limit). */
    public static final int FULL_NOTE_Y = 21;
    public static final int FULL_NOTE_HEIGHT = 126;
    public static final int NOTE_WIDTH = 114;
    /** A note longer than a book page is refused (Minecraft's book page limit is under 1024 characters). */
    public static final int MAX_NOTE_LENGTH = 1024;
    /** Lines at which a page without a photograph turns into a full text page. */
    public static final int FULL_PAGE_LINES = 4;

    /** Plain page drawn over the frame and rules; made from Exposure's album.png only (see tools/album_blank_page.py). */
    public static final Identifier BLANK_PAGE = new Identifier("councilcraft", "textures/gui/album_blank_page.png");
    private static final int BLANK_WIDTH = 298;
    private static final int BLANK_HEIGHT = 188;
    private static final int OVERLAY_U = 24;
    private static final int OVERLAY_V = 20;
    private static final int OVERLAY_WIDTH = 110;
    private static final int OVERLAY_HEIGHT = 140;

    private AlbumTextPages() {
    }

    /** Wrapped height of a note in pixels, as Exposure's TextBox validator measures it. */
    public static int noteHeight(TextRenderer font, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return font.getWrappedLinesHeight(text, NOTE_WIDTH) + (text.endsWith("\n") ? font.fontHeight : 0);
    }

    public static int noteLines(TextRenderer font, String text) {
        return noteHeight(font, text) / font.fontHeight;
    }

    /** Exposure's own rule for a note under a photograph: three lines. */
    public static boolean fitsPhotoPage(TextRenderer font, String text) {
        return text != null && noteHeight(font, text) <= STOCK_NOTE_HEIGHT;
    }

    /** A page with no photograph takes as much as a book page: 14 lines and under 1024 characters. */
    public static boolean fitsTextPage(TextRenderer font, String text) {
        return text != null && text.length() < MAX_NOTE_LENGTH && noteHeight(font, text) <= FULL_NOTE_HEIGHT;
    }

    /**
     * PT14 amendment 2: a page with no photograph whose note is longer than 3 lines can't take a photograph (its
     * slot is not clickable), so the note never overflows the 3-line caption box under a photo.
     */
    public static boolean photoBlocked(boolean hasPhoto, int noteLines) {
        return !hasPhoto && noteLines >= FULL_PAGE_LINES;
    }

    /** Moves linear fade progress toward target (0 or 1) by the elapsed time. */
    public static float approach(float progress, float target, long elapsedMs) {
        float step = elapsedMs / FADE_MS;
        if (progress < target) {
            return Math.min(target, progress + step);
        }
        return Math.max(target, progress - step);
    }

    /** Eased alpha for linear progress. */
    public static float smooth(float progress) {
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        return p * p * (3.0F - 2.0F * p);
    }

    /** Draws the plain page over one page's frame and rules. xOffset is 0 for the left page, 140 for the right. */
    public static void drawBlankPage(DrawContext context, int left, int top, int xOffset, float alpha) {
        if (alpha <= 0.004F) {
            return;
        }
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        context.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
        context.drawTexture(BLANK_PAGE, left + xOffset + OVERLAY_U, top + OVERLAY_V, xOffset + OVERLAY_U, OVERLAY_V,
            OVERLAY_WIDTH, OVERLAY_HEIGHT, BLANK_WIDTH, BLANK_HEIGHT);
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
