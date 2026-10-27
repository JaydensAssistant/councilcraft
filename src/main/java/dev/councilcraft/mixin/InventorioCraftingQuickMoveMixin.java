package dev.councilcraft.mixin;

import de.rubixdev.inventorio.player.InventorioScreenHandler;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT58. Shift-click from Inventorio's crafting result fills the hotbar first (fromLast=true),
 * matching vanilla. Inventorio's quickMoveInner defaults fromLast=false into mainInventoryRange.
 */
@Mixin(value = InventorioScreenHandler.class, remap = false)
public abstract class InventorioCraftingQuickMoveMixin {
    private static final ThreadLocal<Boolean> HOTBAR_FIRST = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "quickMoveInner", at = @At("HEAD"))
    private void councilcraft$markCraftingResult(int sourceIndex, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        InventorioScreenHandler self = (InventorioScreenHandler) (Object) this;
        HOTBAR_FIRST.set(sourceIndex == self.getCraftingResultSlotIndex());
    }

    @Inject(method = "quickMoveInner", at = @At("RETURN"))
    private void councilcraft$clearCraftingResult(int sourceIndex, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        HOTBAR_FIRST.set(Boolean.FALSE);
    }

    @ModifyArg(
        method = "quickMoveInner",
        at = @At(
            value = "INVOKE",
            target = "Lde/rubixdev/inventorio/util/RandomStuff;insertItem$default(Lnet/minecraft/class_1703;Lnet/minecraft/class_1799;Lkotlin/ranges/IntRange;ZILjava/lang/Object;)Z"
        ),
        index = 3
    )
    private boolean councilcraft$fromLastForResult(boolean fromLast) {
        return Boolean.TRUE.equals(HOTBAR_FIRST.get()) ? true : fromLast;
    }
}
