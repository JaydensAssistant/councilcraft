package dev.councilcraft.compat;

import de.rubixdev.inventorio.api.InventorioAPI;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import java.util.List;
import net.mehvahdjukaar.supplementaries.common.items.SackItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Sack weight held in Inventorio's own slots. Inventorio 1.10.4 keeps the Deep Pockets
 * rows (up to 27), the utility belt (up to 8) and the tool belt in a separate
 * {@link PlayerInventoryAddon}, not in the vanilla {@code PlayerInventory}, so
 * Supplementaries never sees them when it weighs the player.
 *
 * <p>Only {@code SackEncumbranceMixin} calls this, and that mixin is applied only when
 * Inventorio is loaded, so this class is never loaded without Inventorio.
 */
public final class InventorioSacks {
    private InventorioSacks() {
    }

    /** The same per-stack weight Supplementaries uses, summed over the addon slots. */
    public static float encumbrance(ServerPlayerEntity player) {
        PlayerInventoryAddon addon = InventorioAPI.getInventoryAddon(player);
        if (addon == null) {
            return 0.0f;
        }
        return sum(addon.deepPockets) + sum(addon.utilityBelt) + sum(addon.toolBelt);
    }

    private static float sum(List<ItemStack> stacks) {
        float amount = 0.0f;
        for (ItemStack stack : stacks) {
            amount += SackItem.getEncumber(stack);
        }
        return amount;
    }
}
