package dev.councilcraft.mixin;

import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.councilcraft.enchant.PrintedCopies;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBehaviour;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.PrintingBehaviour;

/**
 * S207 / S242. The printer will not start on a printed template, and it will
 * not finish one. A copy that does print is marked, including the grey lore line.
 */
@Pseudo
@Mixin(targets = "plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBlockEntity")
public class PrinterCopyMixin {
    @Shadow
    private PrinterBehaviour printer;

    @Redirect(
        method = "startProcessing",
        at = @At(
            value = "INVOKE",
            target = "Lplus/dragons/createenchantmentindustry/common/fluids/printer/behaviour/PrintingBehaviour;isValid()Z"
        )
    )
    private boolean councilcraft$blockPrintedStart(PrintingBehaviour behaviour) {
        if (PrintedCopies.isPrinted(this.printer.getFilter())) {
            return false;
        }
        return behaviour.isValid();
    }

    @Redirect(
        method = "startProcessing",
        at = @At(
            value = "INVOKE",
            target = "Lplus/dragons/createenchantmentindustry/common/fluids/printer/behaviour/PrintingBehaviour;getResult(Lnet/minecraft/class_1937;Lnet/minecraft/class_1799;Lio/github/fabricators_of_create/porting_lib/fluids/FluidStack;)Lnet/minecraft/class_1799"
        ),
        remap = false
    )
    private ItemStack councilcraft$markCopy(PrintingBehaviour behaviour, World world, ItemStack stack, FluidStack fluid) {
        if (PrintedCopies.isPrinted(this.printer.getFilter())) {
            return ItemStack.EMPTY;
        }
        ItemStack result = behaviour.getResult(world, stack, fluid);
        if (result != null && !result.isEmpty()) {
            PrintedCopies.mark(result);
        }
        return result == null ? ItemStack.EMPTY : result;
    }

    @Redirect(
        method = "onItemHeld",
        at = @At(
            value = "INVOKE",
            target = "Lplus/dragons/createenchantmentindustry/common/fluids/printer/behaviour/PrintingBehaviour;create(Lnet/minecraft/world/World;Lcom/simibubi/create/foundation/blockEntity/behaviour/fluid/SmartFluidTankBehaviour;Lnet/minecraft/item/ItemStack;)Lcom/mojang/serialization/DataResult;"
        )
    )
    private DataResult<PrintingBehaviour> councilcraft$blockPrintedFinish(
        World world,
        SmartFluidTankBehaviour tank,
        ItemStack template
    ) {
        return open(world, tank, template);
    }

    static DataResult<PrintingBehaviour> open(World world, SmartFluidTankBehaviour tank, ItemStack template) {
        if (PrintedCopies.isPrinted(template)) {
            return DataResult.error(() -> "CouncilCraft refuses a printed template");
        }
        return PrintingBehaviour.create(world, tank, template);
    }
}
