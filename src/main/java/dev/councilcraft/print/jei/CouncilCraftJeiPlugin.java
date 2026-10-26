package dev.councilcraft.print.jei;

import dev.councilcraft.print.PrintPricing;
import dev.councilcraft.print.ProcessInk;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import plus.dragons.createenchantmentindustry.integration.jei.category.printing.PrintingCategory;
import plus.dragons.createenchantmentindustry.integration.jei.category.printing.PrintingRecipeJEI;

/**
 * PT14. Shows CouncilCraft's photo and signed-album printing in CEI's Printing category.
 * EMI shows JEI categories through JEMI, so this is what EMI players see too.
 */
public final class CouncilCraftJeiPlugin implements IModPlugin {
    private static final long DROPLETS_PER_MB = 81;
    private static final TagKey<Fluid> BLACK_DYE = TagKey.of(RegistryKeys.FLUID, new Identifier("c", "dyes/black"));
    private static final TagKey<Item> PHOTO_PAPERS = TagKey.of(RegistryKeys.ITEM, new Identifier("exposure", "photo_papers"));

    @Override
    public Identifier getPluginUid() {
        return new Identifier("councilcraft", "printing");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PrintingCategory.TYPE, List.of(
            new Photo("photograph_black_and_white", false),
            new Photo("photograph_color", true),
            new Album()
        ));
    }

    private static Item item(String namespace, String path) {
        return Registries.ITEM.get(new Identifier(namespace, path));
    }

    private static ItemStack photo(boolean color) {
        ItemStack stack = new ItemStack(item("exposure", "photograph"));
        NbtCompound tag = new NbtCompound();
        tag.putString("photograph_type", color ? "color" : "black_and_white");
        stack.setNbt(tag);
        return stack;
    }

    private static void blackDye(IRecipeSlotBuilder slot, long droplets) {
        for (RegistryEntry<Fluid> entry : Registries.FLUID.iterateEntries(BLACK_DYE)) {
            if (entry.value().getDefaultState().isStill()) {
                slot.addFluidStack(entry.value(), droplets);
            }
        }
    }

    private static Text gray(String key) {
        return Text.translatable(key).formatted(Formatting.GRAY);
    }

    private record Photo(String name, boolean color) implements PrintingRecipeJEI {
        @Override
        public void setBase(IRecipeSlotBuilder slot) {
            List<ItemStack> papers = new ArrayList<>();
            for (RegistryEntry<Item> entry : Registries.ITEM.iterateEntries(PHOTO_PAPERS)) {
                papers.add(new ItemStack(entry.value()));
            }
            slot.addItemStacks(papers);
        }

        @Override
        public void setTemplate(IRecipeSlotBuilder slot) {
            slot.addItemStack(photo(this.color));
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(gray("gui.councilcraft.jei.printing.photograph_template")));
        }

        @Override
        public void setFluid(IRecipeSlotBuilder slot) {
            long droplets = PrintPricing.PHOTO_MB * DROPLETS_PER_MB;
            if (this.color) {
                slot.addFluidStack(ProcessInk.STILL, droplets);
            } else {
                blackDye(slot, droplets);
            }
            // PT16. Say which ink this photo type takes.
            String ink = this.color ? "gui.councilcraft.printer.needs_process_ink" : "gui.councilcraft.printer.needs_black_dye";
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(gray(ink)));
        }

        @Override
        public void setOutput(IRecipeSlotBuilder slot) {
            slot.addItemStack(photo(this.color));
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(gray("gui.councilcraft.jei.printing.generation")));
        }

        @Override
        public Type getType() {
            return new Type(new Identifier("councilcraft", "printing/" + this.name));
        }
    }

    private record Album() implements PrintingRecipeJEI {
        @Override
        public void setBase(IRecipeSlotBuilder slot) {
            slot.addItemStack(new ItemStack(item("exposure", "album")));
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(gray("gui.councilcraft.jei.printing.album_base")));
        }

        @Override
        public void setTemplate(IRecipeSlotBuilder slot) {
            slot.addItemStack(new ItemStack(item("exposure", "signed_album")));
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(gray("gui.councilcraft.jei.printing.album_template")));
        }

        @Override
        public void setFluid(IRecipeSlotBuilder slot) {
            long droplets = PrintPricing.PHOTO_MB * DROPLETS_PER_MB;
            blackDye(slot, droplets);
            slot.addFluidStack(ProcessInk.STILL, droplets);
            slot.addRichTooltipCallback((view, tooltip) -> {
                tooltip.add(gray("gui.councilcraft.jei.printing.album_cost"));
                tooltip.add(gray("gui.councilcraft.jei.printing.album_ink"));
            });
        }

        @Override
        public void setOutput(IRecipeSlotBuilder slot) {
            slot.addItemStack(new ItemStack(item("exposure", "signed_album")));
            slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(gray("gui.councilcraft.jei.printing.generation")));
        }

        @Override
        public Type getType() {
            return new Type(new Identifier("councilcraft", "printing/signed_album"));
        }
    }
}
