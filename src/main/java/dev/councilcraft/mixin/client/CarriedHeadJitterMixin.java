package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.client.CarriedPose;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tschipp.carryon.client.render.CarryRenderHelper;

/**
 * PT23 (third person). Carry On 2.1.2.7 zeroes only some of the carried entity's previous angles and
 * positions, so EMF / Fresh Animations interpolate the head between stale and current values and it jitters.
 * After Carry On's transforms, set every current and previous angle to 0 and previous position to current,
 * as upstream Carry On PR #997 does. Client only, pinned to Carry On 2.1.2.7, fails safe.
 */
@Mixin(value = CarryRenderHelper.class, remap = false)
public abstract class CarriedHeadJitterMixin {
    @Inject(method = "applyEntityTransformations", at = @At("TAIL"))
    private static void councilcraft$steadyPose(PlayerEntity player, float partialTicks, MatrixStack matrices, Entity entity,
                                                CallbackInfo ci) {
        CarriedPose.steady(entity);
    }
}
