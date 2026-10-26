package dev.councilcraft.print;

import com.mojang.serialization.DataResult;
import dev.councilcraft.enchant.PrintedCopies;
import dev.councilcraft.mixin.client.EnchantedBookCostAccess;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.dragons.createenchantmentindustry.common.fluids.experience.ExperienceHelper;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.EnchantedBookPrintingBehaviour;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.PrintingBehaviour;
import plus.dragons.createenchantmentindustry.config.CEIConfig;
import plus.dragons.createenchantmentindustry.util.CEIFluidUnits;

/**
 * PT27 / PT28 / PT29. One grey "Printing cost: ..." line (PT32 wording) on photographs, albums and enchanted books.
 * Photos and signed albums are priced by the same create() the printer runs (PrintPricing inside);
 * enchanted books by CEI's own EnchantedBookPrintingBehaviour, so the line always matches the printer.
 */
public final class CopyCostTooltip {
    private static final Logger LOGGER = LoggerFactory.getLogger("councilcraft");
    private static final Identifier EXPERIENCE = new Identifier("create_enchantment_industry", "experience");
    private static boolean warned;

    private CopyCostTooltip() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            Text line = line(stack);
            if (line != null) {
                lines.add(line);
            }
        });
    }

    static Text line(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        // PT34. Recipe-viewer example inputs stand for "any original", so they carry no cost line.
        if (isRecipeExample(stack)) {
            return null;
        }
        if (PhotoPrintingBehaviour.isPhotograph(stack)) {
            return PrintedCopies.isPrinted(stack) ? cant() : ink(PhotoPrintingBehaviour.create(null, null, stack));
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (SignedAlbumPrintingBehaviour.ALBUM.equals(id)) {
            return gray(Text.translatable("tooltip.councilcraft.copy.unsigned"));
        }
        if (SignedAlbumPrintingBehaviour.SIGNED_ALBUM.equals(id)) {
            return PrintedCopies.isPrinted(stack) ? cant() : ink(SignedAlbumPrintingBehaviour.create(null, null, stack));
        }
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            return enchantedBook(stack);
        }
        return null;
    }

    private static Text ink(Optional<DataResult<PrintingBehaviour>> created) {
        Optional<PrintingBehaviour> behaviour = created.flatMap(r -> r.result());
        if (behaviour.isEmpty()) {
            return cant();
        }
        int mb;
        boolean color;
        if (behaviour.get() instanceof PhotoPrintingBehaviour photo) {
            mb = photo.copyCostMb();
            color = photo.printsInColor();
        } else if (behaviour.get() instanceof SignedAlbumPrintingBehaviour album) {
            mb = album.copyCostMb();
            color = album.printsInColor();
        } else {
            return null;
        }
        String key = color ? "tooltip.councilcraft.copy.process_ink" : "tooltip.councilcraft.copy.black_dye";
        return gray(Text.translatable(key, PrintPricing.format(mb)));
    }

    /** PT28. CEI's own create() and cost total; printed copies, denied and over-capacity books can't be copied. */
    private static Text enchantedBook(ItemStack stack) {
        World world = MinecraftClient.getInstance().world;
        if (world == null) {
            return null;
        }
        try {
            if (!CEIConfig.fluids().enableEnchantedBookPrinting.get()) {
                return null;
            }
            if (PrintedCopies.isPrinted(stack)) {
                return cant();
            }
            Optional<PrintingBehaviour> behaviour = EnchantedBookPrintingBehaviour.create(world, null, stack).flatMap(r -> r.result());
            if (behaviour.isEmpty()) {
                return cant();
            }
            Fluid experience = Registries.FLUID.get(EXPERIENCE);
            if (experience == Fluids.EMPTY) {
                return null;
            }
            int xp = ((EnchantedBookCostAccess) behaviour.get()).councilcraft$cost();
            long mb = CEIFluidUnits.toMillibuckets(ExperienceHelper.getFluidFromExperience(experience, xp));
            if (!PrintPricing.fitsPrinter(mb, CEIConfig.fluids().printerFluidCapacity.get())) {
                return cant();
            }
            return gray(Text.translatable("tooltip.councilcraft.copy.experience", PrintPricing.format(mb), PrintPricing.format(xp)));
        } catch (RuntimeException | LinkageError e) {
            if (!warned) {
                warned = true;
                LOGGER.warn("CouncilCraft: enchanted book copy cost unavailable ({}); the tooltip line is skipped.", e.toString());
            }
            return null;
        }
    }

    /** PT34. The display-only tag the Printing recipe examples carry. */
    public static boolean isRecipeExample(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().getBoolean(dev.councilcraft.CouncilCraftRules.RECIPE_EXAMPLE_NBT);
    }

    private static Text cant() {
        return gray(Text.translatable("tooltip.councilcraft.copy.cant"));
    }

    private static Text gray(Text text) {
        return text.copy().formatted(Formatting.GRAY);
    }
}
