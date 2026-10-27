package dev.councilcraft.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.enchant.PrintedCopies;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** PT49: the generation lines go on printed copies only; "Printed Copy" stays; NBT is never touched. */
class PrintedTooltipFilterTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    private static ItemStack printed(ItemStack stack) {
        PrintedCopies.mark(stack);
        return stack;
    }

    private static List<Text> lines(String generationKey) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("Some Title"));
        lines.add(Text.translatable("book.byAuthor", "Jayden").formatted(Formatting.GRAY));
        lines.add(Text.translatable(generationKey).formatted(Formatting.GRAY));
        lines.add(Text.literal(CouncilCraftRules.PRINTED_LORE).formatted(Formatting.GRAY));
        return lines;
    }

    private static boolean has(List<Text> lines, String key) {
        return lines.stream().anyMatch(l -> l.getContent() instanceof net.minecraft.text.TranslatableTextContent t && t.getKey().equals(key));
    }

    private static boolean hasPrintedCopy(List<Text> lines) {
        return lines.stream().anyMatch(l -> l.getString().equals("Printed Copy"));
    }

    @Test
    void printedSignedBookLosesOriginalLine() {
        // Printed signed books come out as generation 0, which vanilla labels "Original".
        List<Text> l = lines("book.generation.0");
        assertEquals(1, PrintedTooltipFilter.strip(printed(new ItemStack(Items.WRITTEN_BOOK)), l));
        assertFalse(has(l, "book.generation.0"));
        assertTrue(hasPrintedCopy(l));
        assertTrue(has(l, "book.byAuthor"), "other lines stay");
    }

    @Test
    void everyVanillaGenerationLineGoesOnPrintedBooks() {
        for (int g = 0; g <= 3; g++) {
            List<Text> l = lines("book.generation." + g);
            PrintedTooltipFilter.strip(printed(new ItemStack(Items.WRITTEN_BOOK)), l);
            assertFalse(has(l, "book.generation." + g), "generation " + g);
            assertTrue(hasPrintedCopy(l));
        }
    }

    @Test
    void printedPhotoLosesCopyOfOriginalLine() {
        // An Exposure photograph; the item is not on the test classpath, so a stack of any item carrying the flag stands in:
        // the filter looks only at the printed flag and the line's translation key.
        for (int g = 1; g <= 3; g++) {
            List<Text> l = lines("item.exposure.photograph.generation." + g);
            PrintedTooltipFilter.strip(printed(new ItemStack(Items.PAPER)), l);
            assertFalse(has(l, "item.exposure.photograph.generation." + g));
            assertTrue(hasPrintedCopy(l));
        }
    }

    @Test
    void printedAlbumKeepsPrintedCopyAndOtherLines() {
        List<Text> l = new ArrayList<>();
        l.add(Text.literal("Photo Album"));
        l.add(Text.translatable("item.exposure.album.tooltip.photos_count", 3).formatted(Formatting.GRAY));
        l.add(Text.translatable("book.generation.0").formatted(Formatting.GRAY));
        l.add(Text.literal(CouncilCraftRules.PRINTED_LORE).formatted(Formatting.GRAY));
        PrintedTooltipFilter.strip(printed(new ItemStack(Items.BOOK)), l);
        assertFalse(has(l, "book.generation.0"));
        assertTrue(has(l, "item.exposure.album.tooltip.photos_count"));
        assertTrue(hasPrintedCopy(l));
    }

    @Test
    void unprintedOriginalKeepsItsLine() {
        for (String key : List.of("book.generation.0", "book.generation.1", "item.exposure.photograph.generation.2")) {
            List<Text> l = lines(key);
            assertEquals(0, PrintedTooltipFilter.strip(new ItemStack(Items.WRITTEN_BOOK), l));
            assertTrue(has(l, key), key);
        }
        // A stack that has NBT but no printed flag.
        ItemStack other = new ItemStack(Items.WRITTEN_BOOK);
        other.getOrCreateNbt().putInt("generation", 0);
        List<Text> l = lines("book.generation.0");
        assertEquals(0, PrintedTooltipFilter.strip(other, l));
        assertTrue(has(l, "book.generation.0"));
    }

    @Test
    void stripChangesOnlyTheTooltipNeverTheNbt() {
        ItemStack stack = printed(new ItemStack(Items.WRITTEN_BOOK));
        stack.getNbt().putInt("generation", 0);
        NbtCompound before = stack.getNbt().copy();
        PrintedTooltipFilter.strip(stack, lines("book.generation.0"));
        assertEquals(before, stack.getNbt());
        assertTrue(stack.getNbt().getBoolean(CouncilCraftRules.PRINTED_NBT));
    }

    @Test
    void emptyAndNullTooltipsAreSafe() {
        assertEquals(0, PrintedTooltipFilter.strip(ItemStack.EMPTY, new ArrayList<>()));
        assertEquals(0, PrintedTooltipFilter.strip(printed(new ItemStack(Items.PAPER)), new ArrayList<>()));
    }

    @Test
    void literalTextIsNeverMatchedByEnglishWords() {
        // A custom-named item whose literal name reads "Original" is not a generation line.
        List<Text> l = new ArrayList<>();
        l.add(Text.literal("Original"));
        l.add(Text.literal(CouncilCraftRules.PRINTED_LORE));
        assertEquals(0, PrintedTooltipFilter.strip(printed(new ItemStack(Items.PAPER)), l));
    }

    /** The keys come from Exposure's own lang file: every generation key in it is covered, nothing else is. */
    @Test
    void filterCoversEveryGenerationKeyInExposuresLangFile() throws IOException {
        String jar = System.getProperty("cc.exposureJar");
        assertTrue(jar != null && Path.of(jar).toFile().exists(), "Exposure jar from the pack: " + jar);
        JsonObject lang;
        try (ZipFile zip = new ZipFile(jar)) {
            lang = JsonParser.parseReader(new InputStreamReader(zip.getInputStream(zip.getEntry("assets/exposure/lang/en_us.json")), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        int generation = 0;
        for (Map.Entry<String, com.google.gson.JsonElement> e : lang.entrySet()) {
            boolean isGeneration = e.getKey().contains(".generation.");
            assertEquals(isGeneration, PrintedTooltipFilter.isGenerationKey(e.getKey()), e.getKey());
            if (isGeneration) {
                generation++;
            }
        }
        assertEquals(4, generation, "Exposure photograph generation 0..3");
        assertTrue(PrintedTooltipFilter.isGenerationKey("book.generation.2"));
        assertFalse(PrintedTooltipFilter.isGenerationKey("item.exposure.photograph.photographer_tooltip"));
        assertFalse(PrintedTooltipFilter.isGenerationKey("item.exposure.photograph.tooltip.details.copy"));
        assertFalse(PrintedTooltipFilter.isGenerationKey(null));
    }

    @Test
    void registeredFromTheClientEntrypoint() throws IOException {
        assertTrue(java.nio.file.Files.readString(Path.of("src/main/java/dev/councilcraft/CouncilCraftClient.java"))
            .contains("PrintedTooltipFilter.register();"));
    }
}
