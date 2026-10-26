package dev.councilcraft.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * PT28. CEI's EnchantedBookPrintingBehaviour totals its experience cost (data-map custom costs,
 * CEIEnchantmentHelper and the config multiplier) in its constructor into a private field.
 * The copy-cost tooltip reads that total instead of repeating CEI's formula.
 */
@Mixin(targets = "plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.EnchantedBookPrintingBehaviour")
public interface EnchantedBookCostAccess {
    @Accessor(value = "cost", remap = false)
    int councilcraft$cost();
}
