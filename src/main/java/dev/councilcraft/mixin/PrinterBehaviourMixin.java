package dev.councilcraft.mixin;

import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.councilcraft.enchant.PrintedCopies;
import dev.councilcraft.enchant.PrinterGate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.PrintingBehaviour;

/**
 * S207. A printed stack cannot be installed as a printer template.
 * A template that was saved before this rule is dropped the next time the
 * printer resolves it.
 */
@Pseudo
@Mixin(targets = "plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBehaviour")
public class PrinterBehaviourMixin {
    @Inject(
        method = "setFilter(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void councilcraft$rejectPrinted(ItemStack stack, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (PrintedCopies.isPrinted(stack)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
        method = "resolvePrinting",
        at = @At(
            value = "INVOKE",
            target = "Lplus/dragons/createenchantmentindustry/common/fluids/printer/behaviour/PrintingBehaviour;create(Lnet/minecraft/world/World;Lcom/simibubi/create/foundation/blockEntity/behaviour/fluid/SmartFluidTankBehaviour;Lnet/minecraft/item/ItemStack;)Lcom/mojang/serialization/DataResult;"
        )
    )
    private DataResult<PrintingBehaviour> councilcraft$dropSavedPrinted(
        World world,
        SmartFluidTankBehaviour tank,
        ItemStack template
    ) {
        return PrinterGate.open(world, tank, template);
    }
}
