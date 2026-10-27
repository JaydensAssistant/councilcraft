package dev.councilcraft.compat;

import net.mehvahdjukaar.supplementaries.common.items.FlagItem;
import net.minecraft.block.BannerBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * PT43. Small Ships 2.0.1 only accepts a vanilla {@code BannerItem}. A Supplementaries flag on a ship is stored as
 * the vanilla banner of the same colour with the same patterns and a hidden marker (see {@link FlagBannerNbt}), so
 * Small Ships renders it with its own banner code. Every place Small Ships gives the banner back turns a marked
 * banner into the identical original flag. A banner without the marker is never touched.
 */
public final class FlagBanners {
    private FlagBanners() {
    }

    public static boolean isFlag(ItemStack stack) {
        return stack != null && stack.getItem() instanceof FlagItem;
    }

    /** The marked banner for a flag stack; any other stack comes back as the same instance. */
    public static ItemStack toBanner(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof FlagItem flag)) {
            return stack;
        }
        Item banner = BannerBlock.getForColor(flag.getColor()).asItem();
        return rebuild(stack, banner, FlagBannerNbt.mark(stack.getNbt(), Registries.ITEM.getId(stack.getItem()).toString()));
    }

    /**
     * The original item for a marked banner, with the NBT the flag had before it was marked. Any other stack, or a
     * marker naming an item that is not registered, comes back as the same instance (never lose the stack).
     */
    public static ItemStack restore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !FlagBannerNbt.isMarked(stack.getNbt())) {
            return stack;
        }
        Identifier id = Identifier.tryParse(FlagBannerNbt.originalId(stack.getNbt()));
        if (id == null || !Registries.ITEM.containsId(id)) {
            return stack;
        }
        return rebuild(stack, Registries.ITEM.get(id), FlagBannerNbt.unmark(stack.getNbt()));
    }

    /** The value a Small Ships banner data slot should hold: flags are converted, anything else is unchanged. */
    public static Object forShipSlot(Object value) {
        return value instanceof ItemStack stack ? toBanner(stack) : value;
    }

    /** The value a Small Ships banner hand-back should give: marked banners are restored, anything else is unchanged. */
    public static Object forHandBack(Object value) {
        return value instanceof ItemStack stack ? restore(stack) : value;
    }

    private static ItemStack rebuild(ItemStack from, Item item, NbtCompound nbt) {
        ItemStack out = new ItemStack(item, from.getCount());
        if (nbt != null) {
            out.setNbt(nbt);
        }
        return out;
    }
}
