package dev.councilcraft.print;

import java.util.List;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** PT16. The "which ink" hints on the Printer: goggles (ink tank) and the template slot hover. */
public final class PrintInkHints {
    private PrintInkHints() {
    }

    /** Goggle lines for a Printer with no template, in CEI's goggle layout. */
    public static void goggles(List<Text> tooltip) {
        line("gui.councilcraft.printer.photo_inks.header").forGoggles(tooltip);
        line("gui.councilcraft.printer.photo_inks.black_and_white").style(Formatting.GRAY).forGoggles(tooltip, 1);
        line("gui.councilcraft.printer.photo_inks.color").style(Formatting.GRAY).forGoggles(tooltip, 1);
    }

    private static LangBuilder line(String key) {
        return new LangBuilder("create_enchantment_industry").add(Text.translatable(key));
    }

    /** One extra line under the template slot's hover hint. */
    public static MutableText slotTip() {
        return Text.translatable("gui.councilcraft.printer.photo_inks.slot").formatted(Formatting.GRAY);
    }
}
