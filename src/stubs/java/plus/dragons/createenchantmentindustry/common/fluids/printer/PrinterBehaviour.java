package plus.dragons.createenchantmentindustry.common.fluids.printer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Name only. The real class is inside Create: Enchantment Industry.
 * This file is compiled for the mixin signatures and is not packed into the mod jar.
 */
public class PrinterBehaviour {
    public ItemStack getFilter() {
        return ItemStack.EMPTY;
    }

    public boolean setFilter(ItemStack stack, @Nullable PlayerEntity player) {
        return false;
    }

    public boolean setFilter(ItemStack stack) {
        return false;
    }

    private void resolvePrinting() {
    }
}
