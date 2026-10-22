package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.councilcraft.compat.ParagliderHands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tictim.paraglider.wind.WindUtils;

/** PPM70. Paragliders 20.1.4 places updraft wind (every 4 ticks) only around players with a main-hand paraglider; an offhand one counts too. */
@Mixin(value = WindUtils.class, remap = false)
public abstract class ParagliderOffhandWindMixin {
    @WrapOperation(method = "updateWind", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/player/PlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;",
        remap = true))
    private static ItemStack councilcraft$offhandWind(PlayerEntity player, Operation<ItemStack> original) {
        return ParagliderHands.glider(player, original.call(player));
    }
}
