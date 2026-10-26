package plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour;

import com.mojang.serialization.DataResult;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBlockEntity;

/**
 * Name only. The real interface is inside Create: Enchantment Industry 2.5.2-g (its public printing API).
 * This file is compiled for the signatures and is not packed into the mod jar.
 */
public interface PrintingBehaviour extends IHaveGoggleInformation {
    Registry<PrintingBehaviourProvider> REGISTRY = PrintingBehaviourRegistry.REGISTRY;

    static DataResult<PrintingBehaviour> create(World world, SmartFluidTankBehaviour tank, ItemStack stack) {
        return DataResult.error(() -> "stub");
    }

    default boolean isValid() {
        return true;
    }

    default boolean isSafeNBT() {
        return true;
    }

    int getRequiredItemCount(World world, ItemStack stack);

    long getRequiredFluidAmount(World world, ItemStack stack, FluidStack fluid);

    ItemStack getResult(World world, ItemStack stack, FluidStack fluid);

    void onFinished(World world, BlockPos pos, PrinterBlockEntity printer);

    @FunctionalInterface
    interface Provider {
        Optional<DataResult<PrintingBehaviour>> create(World world, SmartFluidTankBehaviour tank, ItemStack stack);
    }
}
