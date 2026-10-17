package dev.councilcraft.mixin;

import dev.councilcraft.rules.HorseshoeTiers;
import net.hyper_pigeon.horseshoes.items.HorseshoesItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S230. Horseshoes 1.0.3 reads {@code getSpeedBonus()} when it builds the
 * movement modifier. Iron and diamond report the pack's half-strength values
 * from here. Netherite reports 0.1. Gold is unchanged, and loot never rolls it.
 */
@Mixin(HorseshoesItem.class)
public abstract class HorseshoeSpeedMixin {
    @Inject(method = "getSpeedBonus", at = @At("RETURN"), cancellable = true, remap = false)
    private void councilcraft$tierSpeed(CallbackInfoReturnable<Float> cir) {
        String id = Registries.ITEM.getId((Item) (Object) this).toString();
        cir.setReturnValue(HorseshoeTiers.speed(id, cir.getReturnValueF()));
    }
}
