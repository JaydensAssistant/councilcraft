package dev.councilcraft.print;

import dev.councilcraft.enchant.PrintedCopies;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

/**
 * PT49. A printed copy shows "Printed Copy" as its only copy marker. On stacks that carry the councilcraft:printed
 * flag this removes the generation lines the original items add: vanilla written books ("Original", "Copy of Original",
 * "Copy of a Copy", "Tattered") and Exposure photographs ("Copy of original", "Copy of a copy", "Tattered").
 * Lines are matched by translation key, never by English text. Only the tooltip changes: no NBT is read for writing, so
 * the copy rules and Exposure's own copy limits are untouched. Unprinted originals keep their line.
 */
public final class PrintedTooltipFilter {
    /** Vanilla written book generation lines: book.generation.0 .. book.generation.3. */
    static final String VANILLA_PREFIX = "book.generation.";
    /** Exposure photograph generation lines (found in its en_us.json): item.exposure.photograph.generation.N. */
    static final String EXPOSURE_PREFIX = "item.exposure.photograph.generation.";

    private PrintedTooltipFilter() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> strip(stack, lines));
    }

    /** True for a translation key that is a generation line. */
    public static boolean isGenerationKey(String key) {
        return key != null && (key.startsWith(VANILLA_PREFIX) || key.startsWith(EXPOSURE_PREFIX));
    }

    /** Removes generation lines from a printed stack's tooltip. Returns how many were removed. */
    public static int strip(ItemStack stack, List<Text> lines) {
        if (!PrintedCopies.isPrinted(stack)) {
            return 0;
        }
        int before = lines.size();
        lines.removeIf(line -> line.getContent() instanceof TranslatableTextContent t && isGenerationKey(t.getKey()));
        return before - lines.size();
    }
}
