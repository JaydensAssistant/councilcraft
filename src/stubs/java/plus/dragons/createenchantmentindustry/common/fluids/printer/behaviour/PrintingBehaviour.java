package plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour;

import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Name only. The real class is inside Create: Enchantment Industry.
 * This file is compiled for the mixin signatures and is not packed into the mod jar.
 */
public interface PrintingBehaviour {
    boolean isValid();

    ItemStack getResult(World world, ItemStack stack, FluidStack fluid);

    static DataResult<PrintingBehaviour> create(World world, SmartFluidTankBehaviour tank, ItemStack stack) {
        return DataResult.error(() -> "stub");
    }
}
