package dev.councilcraft.enchant;

import dev.councilcraft.CouncilCraftRules;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * S207 / S242 / PPM51. A printed stack carries an NBT flag and a grey lore line.
 * The flag is per stack. An item tag cannot do that, because every enchanted
 * book is the same item.
 */
public final class PrintedCopies {
    private PrintedCopies() {
    }

    public static boolean isPrinted(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasNbt()) {
            return false;
        }
        return stack.getNbt().getBoolean(CouncilCraftRules.PRINTED_NBT);
    }

    public static void mark(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.getOrCreateNbt().putBoolean(CouncilCraftRules.PRINTED_NBT, true);
        NbtCompound display = stack.getOrCreateSubNbt("display");
        NbtList lore = display.getList("Lore", NbtElement.STRING_TYPE);
        String line = loreJson();
        for (int i = 0; i < lore.size(); i++) {
            if (line.equals(lore.getString(i))) {
                display.put("Lore", lore);
                return;
            }
        }
        lore.add(NbtString.of(line));
        display.put("Lore", lore);
    }

    /** True when the stack holds enchantments: applied ones, or stored ones (enchanted books, CEI templates). */
    public static boolean carriesEnchantments(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasNbt()) {
            return false;
        }
        NbtCompound nbt = stack.getNbt();
        return !nbt.getList("Enchantments", NbtElement.COMPOUND_TYPE).isEmpty()
            || !nbt.getList("StoredEnchantments", NbtElement.COMPOUND_TYPE).isEmpty();
    }

    /** True when any of these stacks carries the printed mark. */
    public static boolean anyPrinted(ItemStack... inputs) {
        for (ItemStack input : inputs) {
            if (isPrinted(input)) {
                return true;
            }
        }
        return false;
    }

    /**
     * PPM51 / PM2b copy-wins. A combination that involves a copy produces a copy: the result is printed when any
     * input is printed, otherwise it carries no mark. Two originals still make an original (PM2a).
     */
    public static void copyWins(ItemStack result, ItemStack... inputs) {
        if (result == null || result.isEmpty()) {
            return;
        }
        if (anyPrinted(inputs)) {
            mark(result);
        } else {
            clear(result);
        }
    }

    public static void clear(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasNbt()) {
            return;
        }
        NbtCompound nbt = stack.getNbt();
        nbt.remove(CouncilCraftRules.PRINTED_NBT);
        NbtCompound display = nbt.getCompound("display");
        if (!display.isEmpty()) {
            NbtList lore = display.getList("Lore", NbtElement.STRING_TYPE);
            String line = loreJson();
            NbtList kept = new NbtList();
            for (int i = 0; i < lore.size(); i++) {
                if (!line.equals(lore.getString(i))) {
                    kept.add(NbtString.of(lore.getString(i)));
                }
            }
            if (kept.isEmpty()) {
                display.remove("Lore");
            } else {
                display.put("Lore", kept);
            }
            if (display.isEmpty()) {
                nbt.remove("display");
            }
        }
        if (nbt.isEmpty()) {
            stack.setNbt(null);
        }
    }

    static String loreJson() {
        return Text.Serializer.toJson(
            Text.literal(CouncilCraftRules.PRINTED_LORE).formatted(Formatting.GRAY).styled(style -> style.withItalic(false))
        );
    }
}
