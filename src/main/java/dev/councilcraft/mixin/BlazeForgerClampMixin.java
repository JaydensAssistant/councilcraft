package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.councilcraft.enchant.ForgerLevels;
import dev.councilcraft.enchant.PrintedCopies;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S177 / S207. Clamp the merged level at every store, before Create
 * Enchantment Industry compares it with the level already on the item.
 * A result that clamps back to that level fails the "would not improve"
 * check, so the second item and the XP are not consumed.
 *
 * <p>Two inputs that are both at or under the vanilla max cannot produce a
 * level above that max. Only Sharpness, Power, Blast Protection and Protection
 * may sit one above the max, and only when an input was already above it.
 *
 * <p>Extracting an enchant from a printed stack marks the template that
 * receives it. The source stack is the same object, so it keeps its own mark.
 */
@Pseudo
@Mixin(targets = "plus.dragons.createenchantmentindustry.common.processing.forger.BlazeForgerInventory")
public abstract class BlazeForgerClampMixin {
    @ModifyVariable(method = "applyEnchantments", name = "resultLevel", at = @At("STORE"))
    private int councilcraft$clampResult(
        int resultLevel,
        @Local(name = "baseLevel") int baseLevel,
        @Local(name = "additionLevel") int additionLevel,
        @Local(name = "enchantment") Enchantment enchantment
    ) {
        Identifier id = Registries.ENCHANTMENT.getId(enchantment);
        if (id == null) {
            return resultLevel;
        }
        return ForgerLevels.clamp(
            ForgerLevels.mayExceedVanillaMax(id.toString()),
            enchantment.getMaxLevel(),
            baseLevel,
            additionLevel,
            resultLevel
        );
    }

    @Inject(method = "calculateExtract", at = @At("RETURN"))
    private void councilcraft$carryPrinted(ItemStack base, ItemStack addition, CallbackInfoReturnable<?> cir) {
        Object result = cir.getReturnValue();
        if (result == null || !PrintedCopies.isPrinted(base)) {
            return;
        }
        try {
            Object status = result.getClass().getMethod("status").invoke(result);
            if (status != null && "READY".equals(status.toString())) {
                PrintedCopies.mark(addition);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "CouncilCraft hook failed: printed copies (S207). Blaze Forger result status is missing or was renamed.",
                e
            );
        }
    }
}
