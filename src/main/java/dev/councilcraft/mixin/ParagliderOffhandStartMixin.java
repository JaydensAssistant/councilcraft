package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.councilcraft.compat.ParagliderHands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tictim.paraglider.plugin.ParagliderDefaultPlugin;

/**
 * PPM70. Glide start. In Paragliders 20.1.4 the IDLE to PARAGLIDING state connection is the lambda
 * {@code lambda$registerStateConnections$5}: in the air, not elytra-flying, main-hand stack is a
 * Paraglider and {@code canDoParagliding}. Both main-hand reads take {@link ParagliderHands#glider}
 * instead, so an offhand paraglider starts a glide too. Mixin fails the load if the lambda changes.
 */
@Mixin(value = ParagliderDefaultPlugin.class, remap = false)
public abstract class ParagliderOffhandStartMixin {
    @WrapOperation(method = "lambda$registerStateConnections$5", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/player/PlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;",
        remap = true), require = 2)
    private static ItemStack councilcraft$offhandGlider(PlayerEntity player, Operation<ItemStack> original) {
        return ParagliderHands.glider(player, original.call(player));
    }
}
