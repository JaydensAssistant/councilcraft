package dev.councilcraft.mixin.client;

import dev.councilcraft.client.album.AlbumTextPages;
import io.github.mortuusars.exposure.client.gui.screen.album.AlbumViewScreen;
import io.github.mortuusars.exposure.client.gui.screen.element.TextBlock;
import io.github.mortuusars.exposure.client.gui.screen.element.textbox.HorizontalAlignment;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT14. Signed album and lectern views (Exposure 1.9.22's AlbumViewScreen and its LecternAlbumScreen; pinned in
 * CouncilCraftMixinPlugin). A page with a note and no photograph is drawn full-page: no frame, the note
 * left-aligned at x+22, y+21 (114 x 126) on a plain page.
 */
@Pseudo
@Mixin(targets = "io.github.mortuusars.exposure.client.gui.screen.album.AlbumViewScreen", remap = false)
public abstract class AlbumViewTextPageMixin {
    @Shadow(remap = false)
    @Final
    protected List<AlbumViewScreen.Page> pages;
    @Shadow(remap = false)
    protected int leftPos;
    @Shadow(remap = false)
    protected int topPos;

    @Unique
    private final boolean[] councilcraft$noteOnly = new boolean[2];

    @Inject(method = "method_25394(Lnet/minecraft/class_332;IIF)V", at = @At("HEAD"), remap = false)
    private void councilcraft$layoutNotePages(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        for (int i = 0; i < this.pages.size() && i < 2; i++) {
            AlbumViewScreen.Page page = this.pages.get(i);
            TextBlock block = page.noteWidget();
            ClickableWidget note = (ClickableWidget) (Object) block;
            boolean noteOnly = page.photographWidget().getPhotograph().isEmpty() && !note.getMessage().getString().isBlank();
            this.councilcraft$noteOnly[i] = noteOnly;
            ((ClickableWidget) (Object) page.photographWidget()).visible = !noteOnly;
            int y = this.topPos + (noteOnly ? AlbumTextPages.FULL_NOTE_Y : AlbumTextPages.STOCK_NOTE_Y);
            int height = noteOnly ? AlbumTextPages.FULL_NOTE_HEIGHT : AlbumTextPages.STOCK_NOTE_HEIGHT;
            if (note.getY() != y || note.getHeight() != height) {
                note.setY(y);
                ((ClickableWidgetAccess) note).councilcraft$setHeight(height);
                block.alignment = noteOnly ? HorizontalAlignment.LEFT : HorizontalAlignment.CENTER;
                note.setMessage(note.getMessage());
            }
        }
    }

    @Inject(method = "renderBackground(Lnet/minecraft/class_332;IIF)V", at = @At("TAIL"), remap = false)
    private void councilcraft$drawBlankPages(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        for (int i = 0; i < this.pages.size() && i < 2; i++) {
            if (this.councilcraft$noteOnly[i]) {
                AlbumTextPages.drawBlankPage(context, this.leftPos, this.topPos, i * 140, 1.0F);
            }
        }
    }
}
