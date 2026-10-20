package dev.councilcraft.mixin.client;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.fabric.event.ParagliderClientEventHandler;

/** PM37: Paragliders has no off switch for its stamina wheel; the stamina shows in the XP bar instead. */
@Mixin(value = ParagliderClientEventHandler.class, remap = false)
public abstract class ParagliderWheelMixin {
    @Inject(method = "renderStaminaWheel", at = @At("HEAD"), cancellable = true)
    private static void councilcraft$noWheel(DrawContext context, CallbackInfo ci) {
        ci.cancel();
    }
}
