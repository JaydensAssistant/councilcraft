package dev.councilcraft.mixin.client;

import dev.councilcraft.client.input.UtilityBeltScroll;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM67. Mouse wheel with the utility-scroll modifier held cycles Inventorio's utility belt
 * ({@link UtilityBeltScroll}). {@code Mouse.onMouseScroll} is the only caller of
 * {@code scrollInHotbar}, so this is the wheel and nothing else. Applied only with Inventorio.
 */
@Mixin(PlayerInventory.class)
public abstract class UtilityScrollMixin {
    @Shadow
    @Final
    public PlayerEntity player;

    @Inject(method = "scrollInHotbar", at = @At("HEAD"), cancellable = true, require = 1)
    private void councilcraft$utilityScroll(double scrollAmount, CallbackInfo ci) {
        if (UtilityBeltScroll.scroll(this.player, scrollAmount)) {
            ci.cancel();
        }
    }
}
