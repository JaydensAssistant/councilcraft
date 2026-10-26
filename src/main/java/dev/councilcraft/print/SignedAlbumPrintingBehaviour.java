package dev.councilcraft.print;

import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import java.util.List;
import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBlockEntity;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.PrintingBehaviour;

/**
 * PT14 (+amendment). Signed album template, blank album as base. The copy is a signed album with
 * the same title, author, notes and photos, each photo generation + 1. Unsigned albums and albums
 * holding a Copy of a copy or Tattered photo are refused. 200 mB per page with a photo + 10 mB per
 * page with a note; any color photo means the whole album is paid in process ink.
 */
public final class SignedAlbumPrintingBehaviour implements PrintingBehaviour {
    static final Identifier ALBUM = new Identifier("exposure", "album");
    static final Identifier SIGNED_ALBUM = new Identifier("exposure", "signed_album");
    private static final String SIGNED_CONTENT = "signed_album_content";
    private static final String CONTENT = "album_content";

    private final SmartFluidTankBehaviour tank;
    private final ItemStack result;
    private final String title;
    private final String author;
    private final boolean color;
    private final int cost;

    private SignedAlbumPrintingBehaviour(SmartFluidTankBehaviour tank, ItemStack result, String title, String author, boolean color, int cost) {
        this.tank = tank;
        this.result = result;
        this.title = title;
        this.author = author;
        this.color = color;
        this.cost = cost;
    }

    private static boolean is(ItemStack stack, Identifier id) {
        return id.equals(Registries.ITEM.getId(stack.getItem()));
    }

    /** The page's photograph item tag, or null when the page has no photo. */
    private static NbtCompound photo(NbtCompound page) {
        if (!page.contains("photograph", NbtElement.COMPOUND_TYPE)) {
            return null;
        }
        ItemStack stack = ItemStack.fromNbt(page.getCompound("photograph"));
        return stack.isEmpty() ? null : page.getCompound("photograph");
    }

    private static boolean hasSignedNote(NbtCompound page) {
        if (!page.contains("note", NbtElement.STRING_TYPE)) {
            return false;
        }
        try {
            Text note = Text.Serializer.fromJson(page.getString("note"));
            return note != null && !note.getString().isBlank();
        } catch (RuntimeException e) {
            return !page.getString("note").isBlank();
        }
    }

    public static Optional<DataResult<PrintingBehaviour>> create(World world, SmartFluidTankBehaviour tank, ItemStack template) {
        if (is(template, ALBUM)) {
            return Optional.of(DataResult.error(() -> "gui.councilcraft.printer.album.unsigned"));
        }
        if (!is(template, SIGNED_ALBUM)) {
            return Optional.empty();
        }
        NbtCompound tag = template.getNbt();
        if (tag == null || !tag.contains(SIGNED_CONTENT, NbtElement.COMPOUND_TYPE)) {
            return Optional.of(DataResult.error(() -> "gui.councilcraft.printer.album.invalid"));
        }
        NbtCompound out = tag.copy();
        NbtCompound content = out.getCompound(SIGNED_CONTENT);
        NbtList pages = content.getList("pages", NbtElement.COMPOUND_TYPE);
        int photoPages = 0;
        int notePages = 0;
        boolean color = false;
        for (int i = 0; i < pages.size(); i++) {
            NbtCompound page = pages.getCompound(i);
            NbtCompound photo = photo(page);
            if (photo != null) {
                NbtCompound photoTag = photo.getCompound("tag");
                if (!PhotoPrintingBehaviour.hasFrame(photoTag)) {
                    return Optional.of(DataResult.error(() -> "gui.councilcraft.printer.album.invalid"));
                }
                int generation = photoTag.getInt("photograph_generation");
                if (generation >= 2) {
                    return Optional.of(DataResult.error(() -> "gui.councilcraft.printer.album.generation"));
                }
                photoTag.putInt("photograph_generation", generation + 1);
                photo.put("tag", photoTag);
                color |= PhotoPrintingBehaviour.isColor(photoTag);
                photoPages++;
            }
            if (hasSignedNote(page)) {
                notePages++;
            }
        }
        if (photoPages == 0 && notePages == 0) {
            return Optional.of(DataResult.error(() -> "gui.councilcraft.printer.album.invalid"));
        }
        ItemStack result = new ItemStack(template.getItem());
        result.setNbt(out);
        int cost = PrintPricing.albumCost(photoPages, notePages);
        return Optional.of(DataResult.success(new SignedAlbumPrintingBehaviour(
            tank, result, content.getString("title"), content.getString("author"), color, cost)));
    }

    /** Only an unused album is a base: never consume an album that already holds photos or notes. */
    static boolean isBlankAlbum(ItemStack stack) {
        if (!is(stack, ALBUM)) {
            return false;
        }
        NbtCompound tag = stack.getNbt();
        if (tag == null || !tag.contains(CONTENT, NbtElement.COMPOUND_TYPE)) {
            return true;
        }
        NbtList pages = tag.getCompound(CONTENT).getList("pages", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < pages.size(); i++) {
            NbtCompound page = pages.getCompound(i);
            if (photo(page) != null || !page.getString("note").isBlank()) {
                return false;
            }
        }
        return true;
    }

    /** PT27. Whether this copy is paid in process ink (else black dye). */
    boolean printsInColor() {
        return this.color;
    }

    /** PT27. The cost of one copy in mB, the same number the printer charges. */
    int copyCostMb() {
        return this.cost;
    }

    @Override
    public boolean isValid() {
        return PrintFluids.isValid(this.tank, this.color, this.cost);
    }

    @Override
    public boolean isSafeNBT() {
        return false;
    }

    @Override
    public int getRequiredItemCount(World world, ItemStack stack) {
        return isBlankAlbum(stack) ? 1 : 0;
    }

    @Override
    public long getRequiredFluidAmount(World world, ItemStack stack, FluidStack fluid) {
        return PrintFluids.droplets(fluid, this.color, this.cost);
    }

    @Override
    public ItemStack getResult(World world, ItemStack stack, FluidStack fluid) {
        return this.result.copy();
    }

    @Override
    public void onFinished(World world, BlockPos pos, PrinterBlockEntity printer) {
        world.syncWorldEvent(1043, pos.down(), 0);
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        PrintFluids.goggles(tooltip, this.tank, Text.literal(this.title), this.author, this.color, this.cost);
        return true;
    }
}
