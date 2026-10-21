package dev.councilcraft.mixin;

import dev.councilcraft.compat.CarryOnWeight;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tschipp.carryon.CarryOnCommon;
import tschipp.carryon.common.carry.CarryOnDataManager;

/**
 * PPM35. Carry On 2.1.2.7 adds Slowness every tick in {@code CarryOnCommon.onCarryTick}. Only that
 * addStatusEffect call is redirected, to Supplementaries' overencumbered effect ({@link CarryOnWeight}).
 * Applied only when Carry On and Supplementaries are both loaded (CouncilCraftMixinPlugin).
 */
@Mixin(value = CarryOnCommon.class, remap = false)
public abstract class CarryOnWeightMixin {
    @Inject(method = "onCarryTick", at = @At("HEAD"))
    private static void councilcraft$clearWhenDropped(ServerPlayerEntity player, CallbackInfo ci) {
        CarryOnWeight.tick(player, CarryOnDataManager.getCarryData(player).isCarrying());
    }

    @Redirect(method = "onCarryTick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerPlayerEntity;addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z",
        remap = true))
    private static boolean councilcraft$overencumbered(ServerPlayerEntity player, StatusEffectInstance slowness) {
        return CarryOnWeight.apply(player, slowness);
    }
}
