package dev.councilcraft.mixin.client;

import dev.councilcraft.respawn.RebornTooltip;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client only. Jayden: this file, {@code RebornTooltipDrawMixin} and
 * {@code RebornTooltip} are the tooltip surface.
 *
 * <p>Vanilla's narrow effect list calls {@code DrawContext.drawTooltip}. This
 * replaces that list. EMI's multi-effect row calls the same draw method, so
 * {@link RebornTooltipDrawMixin} covers it without targeting EMI's mixin.
 * EMI's single-effect bar draws the name in the bar and opens no tooltip.
 * That case is drawn at the end of this method, and only when EMI is putting
 * effects along the top.
 */
@Mixin(AbstractInventoryScreen.class)
public abstract class RebornEffectTooltipMixin {
    @ModifyArg(
        method = "drawStatusEffects",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V"
        ),
        index = 1,
        require = 1
    )
    private List<Text> councilcraft$vanillaRebornTooltip(List<Text> lines) {
        return RebornTooltip.linesFor(lines);
    }

    @Inject(method = "drawStatusEffects", at = @At("TAIL"))
    private void councilcraft$emiSingleRebornHover(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        RebornTooltip.hoverSingle(context, (HandledScreen<?>) (Object) this, mouseX, mouseY);
    }
}
