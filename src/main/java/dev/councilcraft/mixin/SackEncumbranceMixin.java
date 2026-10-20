package dev.councilcraft.mixin;

import dev.councilcraft.compat.InventorioSacks;
import net.mehvahdjukaar.supplementaries.common.utils.fabric.ItemsUtilImpl;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sacks in Inventorio's Deep Pockets rows, utility belt and tool belt weigh the player down.
 *
 * <p>Supplementaries 1.20-3.1.44 applies Overencumbered from {@code SackItem.inventoryTick},
 * on the server, every 27 ticks. The weight comes from this method, which only walks the
 * vanilla {@code PlayerInventory} (41 slots) plus a Quark backpack. Inventorio already
 * calls {@code inventoryTick} on its own stacks, so a sack there does run the check; it just
 * is not counted. This adds those stacks to the total. The {@code sack_increment} config
 * and the effect itself stay in Supplementaries. The addon slots are separate from the
 * vanilla inventory, so nothing is counted twice.
 *
 * <p>Applied only when Inventorio is loaded ({@link CouncilCraftMixinPlugin#shouldApply}).
 */
@Mixin(value = ItemsUtilImpl.class, remap = false)
public abstract class SackEncumbranceMixin {
    @Inject(method = "getEncumbermentFromInventory", at = @At("RETURN"), cancellable = true)
    private static void councilcraft$countInventorioSlots(
        ItemStack stack,
        ServerPlayerEntity player,
        CallbackInfoReturnable<Float> cir
    ) {
        cir.setReturnValue(cir.getReturnValueF() + InventorioSacks.encumbrance(player));
    }
}
