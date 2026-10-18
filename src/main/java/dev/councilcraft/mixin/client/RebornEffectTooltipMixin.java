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
 * Client only. Jayden: this file and {@code RebornTooltip} are the tooltip
 * surface. EMI's default puts effects along the top, so a hit box that assumes
 * the vanilla list on the right never sees them. These hooks edit the tooltip
 * the effect list already draws, wherever that list is.
 *
 * <p>The EMI method is added by EMI's own mixin. {@code require = 0} so a
 * pack without EMI still boots. This mixin applies after EMI (lower priority)
 * so that method exists. It does not shadow inventory coordinates.
 */
@Mixin(value = AbstractInventoryScreen.class, priority = 500)
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

    @ModifyArg(
        method = "emi$drawCenteredEffects(Lnet/minecraft/class_332;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_332;method_51437(Lnet/minecraft/class_327;Ljava/util/List;Ljava/util/Optional;II)V"
        ),
        index = 1,
        require = 0,
        remap = false
    )
    private List<Text> councilcraft$emiRebornTooltip(List<Text> lines) {
        return RebornTooltip.linesFor(lines);
    }

    @Inject(
        method = "emi$drawCenteredEffects(Lnet/minecraft/class_332;II)V",
        at = @At("RETURN"),
        require = 0,
        remap = false
    )
    private void councilcraft$emiSingleRebornHover(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        RebornTooltip.hoverSingle(context, (HandledScreen<?>) (Object) this, mouseX, mouseY);
    }
}
