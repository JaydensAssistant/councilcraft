package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.client.StaminaXpBar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PM37: while stamina is showing, the XP bar slot (bar and level number) draws the stamina bar instead.
 * Vanilla only calls renderExperienceBar when there is no jumping mount, so it never meets the horse jump bar.
 */
@Mixin(InGameHud.class)
public abstract class StaminaXpBarMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private int scaledHeight;

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void councilcraft$staminaBar(DrawContext context, int x, CallbackInfo ci) {
        if (StaminaXpBar.shouldShow(this.client)) {
            StaminaXpBar.draw(this.client, context, x, this.scaledHeight);
            ci.cancel();
        }
    }
}
