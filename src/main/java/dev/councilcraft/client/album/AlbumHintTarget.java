package dev.councilcraft.client.album;

/** PT14. Implemented by Exposure's PhotographSlotWidget through AlbumPhotoSlotMixin. */
public interface AlbumHintTarget {
    /** Font colour of the page's note; the "Click to add photo" hint is drawn only once this is set. */
    void councilcraft$setHintColor(int color);
}
