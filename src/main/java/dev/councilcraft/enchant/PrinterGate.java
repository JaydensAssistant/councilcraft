package dev.councilcraft.enchant;

import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.PrintingBehaviour;

/**
 * Shared printer check. Mixin classes must not call each other's static
 * methods: Mixin rejects a non-private static on a mixin class, and a private
 * one is not visible to the other mixin.
 */
public final class PrinterGate {
    private PrinterGate() {
    }

    public static DataResult<PrintingBehaviour> open(World world, SmartFluidTankBehaviour tank, ItemStack template) {
        if (PrintedCopies.isPrinted(template)) {
            return DataResult.error(() -> "CouncilCraft refuses a printed template");
        }
        return PrintingBehaviour.create(world, tank, template);
    }

    /** A printed template produces nothing, and a real copy is marked. */
    public static ItemStack markCopy(PrintingBehaviour behaviour, World world, ItemStack stack, FluidStack fluid, ItemStack template) {
        if (PrintedCopies.isPrinted(template)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = behaviour.getResult(world, stack, fluid);
        if (result != null && !result.isEmpty()) {
            PrintedCopies.mark(result);
        }
        return result == null ? ItemStack.EMPTY : result;
    }
}
