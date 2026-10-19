package dev.councilcraft.mixin.client;

import dev.councilcraft.respawn.RebornTooltip;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Client only. Jayden: EMI and vanilla both end at
 * {@code DrawContext.drawTooltip}. Replacing the list here covers EMI's
 * multi-effect row without injecting into the method EMI's own mixin adds.
 * A list that is already the two-line hover is left as it is.
 */
@Mixin(DrawContext.class)
public class RebornTooltipDrawMixin {
    @ModifyVariable(
        method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private List<Text> councilcraft$rebornTooltip(List<Text> lines) {
        return RebornTooltip.linesFor(lines);
    }
}
