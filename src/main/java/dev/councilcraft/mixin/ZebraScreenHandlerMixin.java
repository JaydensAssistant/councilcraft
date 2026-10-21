package dev.councilcraft.mixin;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.compat.Zebras;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPPM3. The zebra screen: the chest grid reads inventory slots 3+ (armor is 1, horseshoes 2), and shift-click
 * moves items between the player and the zebra by slot owner rather than vanilla's index arithmetic, which assumes
 * the chest starts at index 2. Everything else in the horse screen (saddle, armor and horseshoe slots) is vanilla
 * plus Horseshoes. Other horses are untouched.
 */
@Mixin(HorseScreenHandler.class)
public abstract class ZebraScreenHandlerMixin extends ScreenHandler {
    @Shadow
    @Final
    private Inventory inventory;
    @Shadow
    @Final
    private AbstractHorseEntity entity;

    protected ZebraScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/screen/slot/Slot;<init>(Lnet/minecraft/inventory/Inventory;III)V"), index = 1)
    private int councilcraft$zebraChestSlot(Inventory slotInventory, int index, int x, int y) {
        if (slotInventory == this.inventory && index >= 2 && Zebras.isZebra(this.entity)) {
            return index - 2 + CouncilCraftRules.ZEBRA_CHEST_FIRST_SLOT;
        }
        return index;
    }

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void councilcraft$zebraQuickMove(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!Zebras.isZebra(this.entity) || index < 0 || index >= this.slots.size()) {
            return;
        }
        Slot clicked = this.slots.get(index);
        if (!clicked.hasStack()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        int playerFirst = -1;
        int playerEnd = -1;
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.slots.get(i).inventory instanceof PlayerInventory) {
                if (playerFirst < 0) {
                    playerFirst = i;
                }
                playerEnd = i + 1;
            }
        }
        if (playerFirst < 0) {
            return;
        }
        ItemStack stack = clicked.getStack();
        ItemStack original = stack.copy();
        boolean moved = false;
        if (clicked.inventory != this.inventory) {
            // Player to zebra: saddle, armor and horseshoe slots first (one item each), then the chest.
            for (int i = 0; i < this.slots.size() && !moved; i++) {
                Slot target = this.slots.get(i);
                if (target.inventory == this.inventory && target.getIndex() < CouncilCraftRules.ZEBRA_CHEST_FIRST_SLOT
                    && target.isEnabled() && !target.hasStack() && target.canInsert(stack)) {
                    moved = this.insertItem(stack, i, i + 1, false);
                }
            }
            for (int i = 0; i < this.slots.size() && !moved; i++) {
                Slot target = this.slots.get(i);
                if (target.inventory == this.inventory && target.getIndex() >= CouncilCraftRules.ZEBRA_CHEST_FIRST_SLOT) {
                    int end = i;
                    while (end < this.slots.size() && this.slots.get(end).inventory == this.inventory) {
                        end++;
                    }
                    moved = this.insertItem(stack, i, end, false);
                    break;
                }
            }
            if (!moved) {
                // Vanilla fallback: hotbar to main inventory and back.
                int hotbarStart = playerEnd - 9;
                moved = index >= hotbarStart
                    ? this.insertItem(stack, playerFirst, hotbarStart, false)
                    : this.insertItem(stack, hotbarStart, playerEnd, false);
            }
        } else {
            moved = this.insertItem(stack, playerFirst, playerEnd, true);
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
}
