package dev.councilcraft.mixin.client;

import dev.councilcraft.respawn.RebornClient;
import dev.councilcraft.respawn.RebornStatusEffect;
import dev.councilcraft.respawn.RebornText;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client only. Owner review: hover text for the Reborn effect.
 *
 * <p>Survival and creative inventories both draw their effect list here.
 * The hit box matches {@code AbstractInventoryScreen.drawStatusEffects} in
 * 1.20.1: a 120px row when the window has room, otherwise the 32px icon.
 * The tooltip is the owner's two lines, then the live "Now" line.
 */
@Mixin(AbstractInventoryScreen.class)
public abstract class RebornEffectTooltipMixin {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected int backgroundWidth;
    @Shadow
    public int width;
    @Shadow
    protected TextRenderer textRenderer;
    @Shadow
    protected MinecraftClient client;

    @Inject(method = "drawStatusEffects", at = @At("TAIL"))
    private void councilcraft$rebornTooltip(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.client.player == null) {
            return;
        }
        int left = this.x + this.backgroundWidth + 2;
        int space = this.width - left;
        Collection<StatusEffectInstance> effects = this.client.player.getStatusEffects();
        if (effects.isEmpty() || space < 32) {
            return;
        }
        boolean wide = space >= 120;
        int rowHeight = effects.size() > 5 ? 132 / (effects.size() - 1) : 33;
        int boxWidth = wide ? 120 : 32;
        if (mouseX < left || mouseX > left + boxWidth) {
            return;
        }
        List<StatusEffectInstance> sorted = new ArrayList<>(effects);
        sorted.sort(null);
        int rowTop = this.y;
        StatusEffectInstance hovered = null;
        for (StatusEffectInstance effect : sorted) {
            if (mouseY >= rowTop && mouseY <= rowTop + rowHeight) {
                hovered = effect;
            }
            rowTop += rowHeight;
        }
        if (hovered == null || hovered.getEffectType() != RebornStatusEffect.EFFECT) {
            return;
        }
        List<Text> lines = List.of(
            RebornText.line1(),
            RebornText.line2(),
            RebornText.now(RebornClient.stats(hovered.getDuration()))
        );
        context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
    }
}
