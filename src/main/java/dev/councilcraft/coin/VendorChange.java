package dev.councilcraft.coin;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.CoinChange;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * PM34, applied when a Numismatics price behaviour could not take the exact listed coins from a player
 * paying by hand (no card). Coins in the main inventory, the same slots Numismatics searches, cover the
 * price's total value, and change comes back as the largest coins possible. Coins are told apart by
 * their item id, so no Numismatics class is needed here.
 */
public final class VendorChange {
    /** Numismatics coin ids, smallest first, with the pack values. */
    private static final String[] COINS = {"spur", "bevel", "sprocket", "cog", "crown", "sun"};
    private static final int[] VALUES = {
        CouncilCraftRules.SPUR_VALUE, CouncilCraftRules.BEVEL_VALUE, CouncilCraftRules.SPROCKET_VALUE,
        CouncilCraftRules.COG_VALUE, CouncilCraftRules.CROWN_VALUE, CouncilCraftRules.SUN_VALUE
    };

    private VendorChange() {
    }

    /** Total value of a Numismatics price map (keys are the Coin enum; its name is the coin id). */
    public static int priceValue(Map<?, Integer> prices) {
        int total = 0;
        for (Map.Entry<?, Integer> entry : prices.entrySet()) {
            int index = indexOf(((Enum<?>) entry.getKey()).name());
            if (index < 0) {
                return -1;
            }
            total += VALUES[index] * Math.max(0, entry.getValue());
        }
        return total;
    }

    /** Takes coins worth at least {@code price} and gives the change. False, and nothing taken, if short. */
    public static boolean payByValue(PlayerEntity player, int price) {
        if (price < 0) {
            return false;
        }
        Item[] items = new Item[COINS.length];
        for (int i = 0; i < COINS.length; i++) {
            items[i] = Registries.ITEM.get(new Identifier("numismatics", COINS[i]));
        }
        List<ItemStack> main = player.getInventory().main;
        int[] available = new int[COINS.length];
        for (ItemStack stack : main) {
            int index = indexOf(items, stack);
            if (index >= 0) {
                available[index] += stack.getCount();
            }
        }
        int[] taken = CoinChange.take(VALUES, available, price);
        if (taken == null) {
            return false;
        }
        int[] toRemove = taken.clone();
        for (ItemStack stack : main) {
            int index = indexOf(items, stack);
            if (index >= 0 && toRemove[index] > 0) {
                int n = Math.min(toRemove[index], stack.getCount());
                stack.decrement(n);
                toRemove[index] -= n;
            }
        }
        int[] change = CoinChange.change(VALUES, CoinChange.value(VALUES, taken) - price);
        for (int i = change.length - 1; i >= 0; i--) {
            int left = change[i];
            while (left > 0) {
                int n = Math.min(left, items[i].getMaxCount());
                player.getInventory().offerOrDrop(new ItemStack(items[i], n));
                left -= n;
            }
        }
        return true;
    }

    private static int indexOf(String coinName) {
        for (int i = 0; i < COINS.length; i++) {
            if (COINS[i].equalsIgnoreCase(coinName)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOf(Item[] items, ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < items.length; i++) {
            if (stack.isOf(items[i])) {
                return i;
            }
        }
        return -1;
    }
}
