package dev.councilcraft.mixin;

import de.rubixdev.inventorio.player.InventorioScreenHandler;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT59 / PT64. SWAP (button 40) on a crafting result is refused (no dupe). On any other
 * non-Inventorio screen, SWAP moves the hovered stack to Inventorio's selected utility belt
 * slot instead of vanilla offhand 40.
 */
@Mixin(ScreenHandler.class)
public abstract class UtilityBeltSwapMixin {
    @Shadow
    public abstract Slot getSlot(int index);

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void councilcraft$utilityOrBlockSwap(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (actionType != SlotActionType.SWAP || button != 40 || player.getWorld().isClient) {
            return;
        }
        if (slotIndex < 0) {
            return;
        }
        ScreenHandler self = (ScreenHandler) (Object) this;
        Slot slot;
        try {
            slot = this.getSlot(slotIndex);
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        // PT64: never SWAP on a crafting result (Inventorio 2x2 or any crafting table).
        if (slot.inventory instanceof CraftingResultInventory
            || (self instanceof InventorioScreenHandler inv && slotIndex == inv.getCraftingResultSlotIndex())) {
            ci.cancel();
            return;
        }
        // PT59: Inventorio's own screen already routes F via PPM84 / inventorio swap — leave it.
        if (self instanceof InventorioScreenHandler) {
            return;
        }
        PlayerInventoryAddon addon = PlayerInventoryAddon.Companion.getInventoryAddon(player);
        if (addon == null) {
            return;
        }
        ItemStack hovered = slot.getStack();
        ItemStack util = addon.getSelectedUtilityStack();
        // Swap without touching vanilla offhand 40.
        addon.setSelectedUtilityStack(hovered.copy());
        slot.setStack(util.copy());
        slot.markDirty();
        self.sendContentUpdates();
        ci.cancel();
    }
}
