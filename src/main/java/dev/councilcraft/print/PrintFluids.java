package dev.councilcraft.print;

import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import java.util.List;
import java.util.OptionalInt;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/** Which fluid pays for a print, and the goggle lines shared by the photo and album behaviours. */
final class PrintFluids {
    /** Create Dragons Plus puts its black dye fluid in this tag. */
    static final TagKey<Fluid> BLACK_DYE = TagKey.of(RegistryKeys.FLUID, new Identifier("c", "dyes/black"));
    private static final long DROPLETS_PER_MB = 81;

    private PrintFluids() {
    }

    /** Cost in mB when the fluid is right for the print, empty for the wrong fluid. */
    static OptionalInt cost(FluidStack fluid, boolean color, int cost) {
        if (fluid.isEmpty()) {
            return OptionalInt.empty();
        }
        boolean right = color ? ProcessInk.is(fluid.getFluid()) : fluid.getFluid().getDefaultState().isIn(BLACK_DYE);
        return right ? OptionalInt.of(cost) : OptionalInt.empty();
    }

    static int capacityMb(SmartFluidTankBehaviour tank) {
        return (int) (tank.getPrimaryHandler().getCapacity() / DROPLETS_PER_MB);
    }

    static boolean isValid(SmartFluidTankBehaviour tank, boolean color, int cost) {
        FluidStack fluid = tank.getPrimaryHandler().getFluid();
        if (fluid.isEmpty()) {
            return true;
        }
        OptionalInt c = cost(fluid, color, cost);
        return c.isPresent() && PrintPricing.fitsPrinter(c.getAsInt(), capacityMb(tank));
    }

    static long droplets(FluidStack fluid, boolean color, int cost) {
        return cost(fluid, color, cost).orElse(0) * DROPLETS_PER_MB;
    }

    private static LangBuilder cei() {
        return new LangBuilder("create_enchantment_industry");
    }

    /** Same layout as CEI's written-book goggles: header, title, author, ink, wrong-liquid warning, cost. */
    static void goggles(List<Text> tooltip, SmartFluidTankBehaviour tank, Text title, String author, boolean color, int cost) {
        cei().translate("gui.goggles.printing").forGoggles(tooltip);
        cei().add(title.copy()).style(Formatting.GRAY).forGoggles(tooltip, 1);
        if (author != null) {
            cei().add(Text.translatable("book.byAuthor", author)).style(Formatting.GRAY).forGoggles(tooltip, 1);
        }
        cei().add(Text.translatable(color ? "gui.councilcraft.printer.needs_process_ink" : "gui.councilcraft.printer.needs_black_dye"))
            .style(Formatting.GRAY).forGoggles(tooltip, 1);
        // PT25. The cost line shows whatever the tank holds. Green or red says whether one copy fits in the
        // printer's tank, which is true for an empty tank too; the wrong ink adds CEI's red warning above it.
        FluidStack fluid = tank.getPrimaryHandler().getFluid();
        if (!fluid.isEmpty() && cost(fluid, color, cost).isEmpty()) {
            cei().translate("gui.goggles.printing.incorrect_liquid").style(Formatting.RED).forGoggles(tooltip);
        }
        cei().translate("gui.goggles.printing.cost",
                CreateLang.number(cost).add(CreateLang.translate("generic.unit.millibuckets")))
            .style(PrintPricing.fitsPrinter(cost, capacityMb(tank)) ? Formatting.GREEN : Formatting.RED)
            .forGoggles(tooltip, 1);
    }
}
