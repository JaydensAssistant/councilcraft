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
 * S230 / PPM4. Horseshoes 1.0.3 reads {@code getSpeedBonus()} and {@code getArmorBonus()} when it builds
 * the movement and armor modifiers. Iron, gold, diamond and netherite report the pack's values from
 * {@link HorseshoeTiers}.
 */
@Mixin(HorseshoesItem.class)
public abstract class HorseshoeStatsMixin {
    @Inject(method = "getSpeedBonus", at = @At("RETURN"), cancellable = true, remap = false)
    private void councilcraft$tierSpeed(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(HorseshoeTiers.speed(councilcraft$id(), cir.getReturnValueF()));
    }

    @Inject(method = "getArmorBonus", at = @At("RETURN"), cancellable = true, remap = false)
    private void councilcraft$tierArmor(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(HorseshoeTiers.armor(councilcraft$id(), cir.getReturnValueF()));
    }

    private String councilcraft$id() {
        return Registries.ITEM.getId((Item) (Object) this).toString();
    }
}
