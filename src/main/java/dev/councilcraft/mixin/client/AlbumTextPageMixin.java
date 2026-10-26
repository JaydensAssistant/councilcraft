package dev.councilcraft.mixin.client;

import dev.councilcraft.client.album.AlbumHintTarget;
import dev.councilcraft.client.album.AlbumTextPages;
import io.github.mortuusars.exposure.client.gui.screen.album.AlbumScreen;
import io.github.mortuusars.exposure.client.gui.screen.album.PhotographSlotWidget;
import io.github.mortuusars.exposure.client.gui.screen.element.TextBlock;
import io.github.mortuusars.exposure.client.gui.screen.element.textbox.HorizontalAlignment;
import io.github.mortuusars.exposure.client.gui.screen.element.textbox.TextBox;
import io.github.mortuusars.exposure.world.inventory.AlbumMenu;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.item.TooltipData;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT14. Text-only pages in Exposure 1.9.22's editable album screen (pinned in CouncilCraftMixinPlugin).
 * A page with no photograph keeps Exposure's layout for 1-3 note lines. A 4th line fades the empty frame out
 * and moves the note to the page body (x+22, y+21, 114 x 126, left-aligned); fewer lines fade the frame back.
 * A page with a photograph keeps Exposure's 3-line limit; a page without one can't take a photograph while its note
 * is over 3 lines (amendment 2). Non-editable pages with only a note are shown full-page.
 * Method names are intermediary because Exposure's jar is not remapped by this project (remap = false).
 */
@Pseudo
@Mixin(targets = "io.github.mortuusars.exposure.client.gui.screen.album.AlbumScreen", remap = false)
public abstract class AlbumTextPageMixin {
    @Shadow(remap = false)
    @Final
    protected List<AlbumScreen.Page> pages;

    @Shadow(remap = false)
    protected abstract boolean isInAddingMode();

    /** Linear fade progress per page: 1 = frame shown, 0 = full text page. */
    @Unique
    private final float[] councilcraft$progress = {1.0F, 1.0F};
    @Unique
    private final Object[] councilcraft$seenPages = new Object[2];
    @Unique
    private int councilcraft$spread = -1;
    @Unique
    private long councilcraft$lastMs;

    @Inject(method = "createPage", at = @At("RETURN"), remap = false)
    private void councilcraft$textPageLimits(CallbackInfoReturnable<AlbumScreen.Page> cir) {
        AlbumScreen.Page page = cir.getReturnValue();
        PhotographSlotWidget photo = page.photographWidget;
        Optional<TextBox> box = page.noteWidget.left();
        if (box.isEmpty()) {
            return;
        }
        TextBox textBox = box.get();
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        textBox.textValidator = text -> photo.getPhotograph().isEmpty()
            ? AlbumTextPages.fitsTextPage(font, text)
            : AlbumTextPages.fitsPhotoPage(font, text);
        ((AlbumHintTarget) (Object) photo).councilcraft$setHintColor(textBox.fontColor);
    }

    @Inject(method = "updateWidgetVisibility", at = @At("TAIL"), remap = false)
    private void councilcraft$layoutTextPages(CallbackInfo ci) {
        long now = Util.getMeasuringTimeMs();
        long elapsed = this.councilcraft$lastMs == 0L ? 0L : Math.min(now - this.councilcraft$lastMs, 100L);
        this.councilcraft$lastMs = now;
        int spread = ((AlbumMenu) (Object) ((HandledScreen<?>) (Object) this).getScreenHandler()).getCurrentSpreadIndex();
        boolean spreadChanged = spread != this.councilcraft$spread;
        this.councilcraft$spread = spread;
        boolean adding = this.isInAddingMode();
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        for (int i = 0; i < this.pages.size(); i++) {
            AlbumScreen.Page page = this.pages.get(i);
            boolean snap = spreadChanged || this.councilcraft$seenPages[i] != page;
            this.councilcraft$seenPages[i] = page;
            ClickableWidget photo = (ClickableWidget) (Object) page.photographWidget;
            boolean hasPhoto = !page.photographWidget.getPhotograph().isEmpty();
            int top = page.pageArea.getY();

            Optional<TextBox> box = page.noteWidget.left();
            if (box.isPresent()) {
                TextBox textBox = box.get();
                boolean blocked = AlbumTextPages.photoBlocked(hasPhoto, AlbumTextPages.noteLines(font, textBox.getText()));
                boolean full = !adding && blocked;
                councilcraft$placeTextBox(textBox, page, top, full);
                float target = full ? 0.0F : 1.0F;
                this.councilcraft$progress[i] = snap ? target : AlbumTextPages.approach(this.councilcraft$progress[i], target, elapsed);
                float alpha = AlbumTextPages.smooth(this.councilcraft$progress[i]);
                photo.setAlpha(alpha);
                // Amendment 2: no photo can be added while the note is over 3 lines (the note tooltip says why).
                photo.active = !blocked;
                if (full && alpha <= 0.0F) {
                    photo.visible = false;
                }
            } else {
                TextBlock block = page.noteWidget.right().orElseThrow();
                boolean noteOnly = !hasPhoto && !((ClickableWidget) (Object) block).getMessage().getString().isBlank();
                councilcraft$placeTextBlock(block, page, top, noteOnly);
                this.councilcraft$progress[i] = noteOnly ? 0.0F : 1.0F;
            }
        }
    }

    @Unique
    private static void councilcraft$placeTextBox(TextBox textBox, AlbumScreen.Page page, int top, boolean full) {
        ClickableWidget widget = (ClickableWidget) (Object) textBox;
        int y = top + (full ? AlbumTextPages.FULL_NOTE_Y : AlbumTextPages.STOCK_NOTE_Y);
        int height = full ? AlbumTextPages.FULL_NOTE_HEIGHT : AlbumTextPages.STOCK_NOTE_HEIGHT;
        if (widget.getY() == y && widget.getHeight() == height) {
            return;
        }
        widget.setY(y);
        textBox.horizontalAlignment = full ? HorizontalAlignment.LEFT : HorizontalAlignment.CENTER;
        textBox.setHeight(height);
        page.noteArea.setY(y);
        page.noteArea.setHeight(height);
        textBox.refresh();
    }

    @Unique
    private static void councilcraft$placeTextBlock(TextBlock block, AlbumScreen.Page page, int top, boolean full) {
        ClickableWidget widget = (ClickableWidget) (Object) block;
        int y = top + (full ? AlbumTextPages.FULL_NOTE_Y : AlbumTextPages.STOCK_NOTE_Y);
        int height = full ? AlbumTextPages.FULL_NOTE_HEIGHT : AlbumTextPages.STOCK_NOTE_HEIGHT;
        if (widget.getY() == y && widget.getHeight() == height) {
            return;
        }
        widget.setY(y);
        ((ClickableWidgetAccess) widget).councilcraft$setHeight(height);
        block.alignment = full ? HorizontalAlignment.LEFT : HorizontalAlignment.CENTER;
        page.noteArea.setY(y);
        page.noteArea.setHeight(height);
        widget.setMessage(widget.getMessage());
    }

    /** Plain page over the frame and rules, as the frame fades out (drawBackground, after Exposure's texture). */
    @Inject(method = "method_2389(Lnet/minecraft/class_332;FII)V", at = @At("TAIL"), remap = false)
    private void councilcraft$drawBlankPages(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        int left = ((HandledScreenAccess) (Object) this).councilcraft$x();
        int top = ((HandledScreenAccess) (Object) this).councilcraft$y();
        for (int i = 0; i < this.pages.size(); i++) {
            int xOffset = this.pages.get(i).pageArea.getX() - left;
            AlbumTextPages.drawBlankPage(context, left, top, xOffset, 1.0F - AlbumTextPages.smooth(this.councilcraft$progress[i]));
        }
    }

    /** Amendment 2: on a page whose note is too long for a photo, the note tooltip says how to add one (drawMouseoverTooltip). */
    @Redirect(method = "method_2380(Lnet/minecraft/class_332;II)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/class_332;method_51437(Lnet/minecraft/class_327;Ljava/util/List;Ljava/util/Optional;II)V"),
        remap = false)
    @SuppressWarnings("unchecked")
    private void councilcraft$noteTooltip(DrawContext context, TextRenderer font, List<Text> lines,
            Optional<TooltipData> data, int x, int y) {
        for (int i = 0; i < this.pages.size(); i++) {
            AlbumScreen.Page page = this.pages.get(i);
            Optional<TextBox> box = page.noteWidget.left();
            if (box.isEmpty() || !page.noteArea.contains(x, y)) {
                continue;
            }
            boolean hasPhoto = !page.photographWidget.getPhotograph().isEmpty();
            if (AlbumTextPages.photoBlocked(hasPhoto, AlbumTextPages.noteLines(font, box.get().getText()))) {
                lines = new java.util.ArrayList<>(lines);
                lines.add(Text.translatable("gui.councilcraft.album.hint.shorten_for_photo").formatted(Formatting.GRAY));
            }
            break;
        }
        context.drawTooltip(font, lines, data, x, y);
    }

    /** A hidden or text-page frame keeps its last hover state; do not show its tooltip (drawMouseoverTooltip). */
    @Redirect(method = "method_2380(Lnet/minecraft/class_332;II)V",
        at = @At(value = "INVOKE",
            target = "Lio/github/mortuusars/exposure/client/gui/screen/album/PhotographSlotWidget;method_25367()Z"),
        remap = false)
    private boolean councilcraft$liveFrameHover(PhotographSlotWidget widget) {
        ClickableWidget frame = (ClickableWidget) (Object) widget;
        return frame.visible && frame.active && frame.isSelected();
    }
}
