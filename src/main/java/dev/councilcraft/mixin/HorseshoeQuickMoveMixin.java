package dev.councilcraft.mixin;

import dev.councilcraft.CouncilCraftRules;
import net.hyper_pigeon.horseshoes.items.HorseshoesItem;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM3. Shift-clicking horseshoes in the horse screen puts them in the empty horseshoe slot (which
 * Horseshoes 1.0.3 adds). With the slot full, the shoes move between hotbar and main inventory as in
 * vanilla. Shift-clicking the horseshoe slot moves the shoes back to the player's inventory.
 * Any other item, or a screen whose layout we do not recognise, falls through to vanilla.
 */
@Mixin(HorseScreenHandler.class)
public abstract class HorseshoeQuickMoveMixin extends ScreenHandler {
    @Shadow
    @Final
    private Inventory inventory;
    @Shadow
    @Final
    private AbstractHorseEntity entity;

    protected HorseshoeQuickMoveMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void councilcraft$quickMoveShoes(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < 0 || index >= this.slots.size()) {
            return;
        }
        Slot clicked = this.slots.get(index);
        if (!clicked.hasStack() || !(clicked.getStack().getItem() instanceof HorseshoesItem)) {
            return;
        }
        int shoeSlot = councilcraft$shoeSlot();
        int[] range = councilcraft$playerRange();
        if (shoeSlot < 0 || range == null) {
            return;
        }
        ItemStack stack = clicked.getStack();
        ItemStack original = stack.copy();
        boolean moved;
        if (index == shoeSlot) {
            moved = this.insertItem(stack, range[0], range[1], true);
        } else if (clicked.inventory instanceof PlayerInventory) {
            Slot target = this.slots.get(shoeSlot);
            if (!target.hasStack() && target.canInsert(stack)) {
                moved = this.insertItem(stack, shoeSlot, shoeSlot + 1, false);
            } else {
                // Vanilla's fallback: hotbar to main inventory and back.
                int hotbarStart = range[1] - 9;
                moved = index >= hotbarStart
                    ? this.insertItem(stack, range[0], hotbarStart, false)
                    : this.insertItem(stack, hotbarStart, range[1], false);
            }
        } else {
            return;
        }
        if (!moved) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) {
            clicked.setStack(ItemStack.EMPTY);
        } else {
            clicked.markDirty();
        }
        cir.setReturnValue(original);
    }

    /** Screen slot index of the horseshoe slot, or -1. */
    @Unique
    private int councilcraft$shoeSlot() {
        int shoeIndex = this.entity.hasArmorSlot()
            ? CouncilCraftRules.HORSESHOE_SLOT_WITH_ARMOR
            : CouncilCraftRules.HORSESHOE_SLOT_WITHOUT_ARMOR;
        for (int i = this.slots.size() - 1; i >= 0; i--) {
            Slot slot = this.slots.get(i);
            if (slot.inventory == this.inventory && slot.getIndex() == shoeIndex) {
                return i;
            }
        }
        return -1;
    }

    /** First and end (exclusive) screen index of the 36 player slots, or null for an unknown layout. */
    @Unique
    private int[] councilcraft$playerRange() {
        int first = -1;
        int count = 0;
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.slots.get(i).inventory instanceof PlayerInventory) {
                if (first < 0) {
                    first = i;
                }
                count++;
            } else if (first >= 0) {
                break;
            }
        }
        return first >= 0 && count == 36 ? new int[] {first, first + 36} : null;
    }
}
