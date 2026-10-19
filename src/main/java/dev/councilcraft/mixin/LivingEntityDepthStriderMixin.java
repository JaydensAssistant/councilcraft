package dev.councilcraft.mixin;

import dev.councilcraft.rules.DepthStriderRule;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * S228. {@code LivingEntity.travel} asks {@code EnchantmentHelper.getDepthStrider}
 * and then, separately, applies Dolphin's Grace. Returning 0 from the enchantment
 * lookup while Grace is active removes the Depth Strider term and leaves Grace's
 * own 0.96 water friction in place. Grace alone and Depth Strider alone still
 * take the vanilla path.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDepthStriderMixin {
    @Redirect(
        method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/enchantment/EnchantmentHelper;getDepthStrider(Lnet/minecraft/entity/LivingEntity;)I"
        )
    )
    private int councilcraft$ignoreDepthStriderUnderGrace(LivingEntity entity) {
        return DepthStriderRule.effectiveLevel(
            EnchantmentHelper.getDepthStrider(entity),
            entity.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)
        );
    }
}
