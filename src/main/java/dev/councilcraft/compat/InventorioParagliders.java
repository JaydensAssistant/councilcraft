package dev.councilcraft.compat;

import de.rubixdev.inventorio.api.InventorioAPI;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import dev.councilcraft.rules.ParagliderHand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import tictim.paraglider.api.item.Paraglider;

/**
 * PPM70. Paragliders clears the Paragliding flag only on the 41 vanilla inventory slots. Inventorio's
 * utility belt is outside them, so a paraglider used from the off hand (the selected belt slot) and
 * then scrolled away would keep showing as open. This clears it on every belt slot except the one in use.
 * Called only when Inventorio is loaded, so this class is never loaded without it.
 */
public final class InventorioParagliders {
    private InventorioParagliders() {
    }

    public static void syncBeltFlags(PlayerEntity player, ItemStack inUse, boolean paragliding) {
        PlayerInventoryAddon addon = InventorioAPI.getInventoryAddon(player);
        if (addon == null) {
            return;
        }
        for (ItemStack stack : addon.utilityBelt) {
            if (stack.getItem() instanceof Paraglider p) {
                p.setParagliding(stack, ParagliderHand.flagged(stack == inUse, paragliding));
            }
        }
    }
}
