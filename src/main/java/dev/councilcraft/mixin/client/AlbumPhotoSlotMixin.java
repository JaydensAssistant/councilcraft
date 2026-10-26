package dev.councilcraft.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.councilcraft.client.album.AlbumHintTarget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT14. Exposure 1.9.22's album photo frame (pinned in CouncilCraftMixinPlugin). The empty frame fades with the
 * widget alpha AlbumTextPageMixin sets, and an empty editable frame shows a hint: "Click to add photo" (70%),
 * then "Keep writing for a / full text page" (45%). Frames holding a photograph always draw at full alpha.
 */
@Pseudo
@Mixin(targets = "io.github.mortuusars.exposure.client.gui.screen.album.PhotographSlotWidget", remap = false)
public abstract class AlbumPhotoSlotMixin extends ClickableWidget implements AlbumHintTarget {
    @Unique
    private static final String[] COUNCILCRAFT$HINT_KEYS = {
        "gui.councilcraft.album.hint.add_photo", null,
        "gui.councilcraft.album.hint.keep_writing", "gui.councilcraft.album.hint.full_text_page"};
    @Unique
    private static final float[] COUNCILCRAFT$HINT_OPACITY = {0.70F, 0.0F, 0.45F, 0.45F};
    /** Hint top, relative to the frame (page y+57; the frame sits at page y+21). */
    @Unique
    private static final int COUNCILCRAFT$HINT_TOP = 36;

    @Shadow(remap = false)
    protected boolean editable;

    @Unique
    private int councilcraft$hintColor;
    @Unique
    private boolean councilcraft$faded;

    private AlbumPhotoSlotMixin() {
        super(0, 0, 0, 0, Text.empty());
    }

    @Shadow(remap = false)
    public abstract ItemStack getPhotograph();

    @Override
    public void councilcraft$setHintColor(int color) {
        this.councilcraft$hintColor = color | 0xFF000000;
    }

    @Inject(method = "method_48579(Lnet/minecraft/class_332;IIF)V", at = @At("HEAD"), remap = false)
    private void councilcraft$fadeFrame(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.councilcraft$faded = this.alpha < 1.0F && this.getPhotograph().isEmpty();
        if (this.councilcraft$faded) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.setShaderColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, this.alpha));
        }
    }

    @Inject(method = "method_48579(Lnet/minecraft/class_332;IIF)V", at = @At("TAIL"), remap = false)
    private void councilcraft$drawHint(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.councilcraft$faded) {
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        if (this.councilcraft$hintColor == 0 || !this.editable || !this.getPhotograph().isEmpty()) {
            return;
        }
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int centerX = this.getX() + this.getWidth() / 2;
        for (int i = 0; i < COUNCILCRAFT$HINT_KEYS.length; i++) {
            if (COUNCILCRAFT$HINT_KEYS[i] == null) {
                continue;
            }
            float opacity = COUNCILCRAFT$HINT_OPACITY[i] * Math.min(1.0F, this.alpha);
            if (opacity < 0.02F) {
                continue;
            }
            int color = (this.councilcraft$hintColor & 0xFFFFFF) | (Math.round(opacity * 255.0F) << 24);
            Text line = Text.translatable(COUNCILCRAFT$HINT_KEYS[i]);
            int y = this.getY() + COUNCILCRAFT$HINT_TOP + i * font.fontHeight;
            context.drawText(font, line, centerX - font.getWidth(line) / 2, y, color, false);
        }
    }
}
