package dev.councilcraft.print;

import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import java.util.List;
import java.util.Optional;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBlockEntity;
import plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour.PrintingBehaviour;

/**
 * PT14. Photograph (or aged photograph) template, one photo paper as base. The copy keeps the
 * same exposure with photograph_generation + 1; Copy of a copy and Tattered photos are refused
 * (Exposure's own rule). 200 mB black dye fluid for black-and-white, 200 mB process ink for color.
 */
public final class PhotoPrintingBehaviour implements PrintingBehaviour {
    static final TagKey<Item> PHOTO_PAPERS = TagKey.of(RegistryKeys.ITEM, new Identifier("exposure", "photo_papers"));
    static final Identifier PHOTOGRAPH = new Identifier("exposure", "photograph");
    static final Identifier AGED_PHOTOGRAPH = new Identifier("exposure", "aged_photograph");

    private final SmartFluidTankBehaviour tank;
    private final ItemStack result;
    private final boolean color;
    private final String author;

    private PhotoPrintingBehaviour(SmartFluidTankBehaviour tank, ItemStack result, boolean color, String author) {
        this.tank = tank;
        this.result = result;
        this.color = color;
        this.author = author;
    }

    static boolean isPhotograph(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return PHOTOGRAPH.equals(id) || AGED_PHOTOGRAPH.equals(id);
    }

    /** Exposure stores "black_and_white" or "color"; anything else (chromatic included) prints in color. */
    static boolean isColor(NbtCompound photoTag) {
        String type = photoTag.contains("photograph_type", NbtElement.STRING_TYPE)
            ? photoTag.getString("photograph_type")
            : photoTag.getCompound("photograph_frame").getString("type");
        return !"black_and_white".equals(type);
    }

    /** Photographer name from the frame, or null when the photo has none. */
    static String photographer(NbtCompound photoTag) {
        String name = photoTag.getCompound("photograph_frame").getCompound("photographer").getString("name");
        return name.isEmpty() ? null : name;
    }

    static boolean hasFrame(NbtCompound photoTag) {
        return photoTag != null && photoTag.contains("photograph_frame", NbtElement.COMPOUND_TYPE);
    }

    public static Optional<DataResult<PrintingBehaviour>> create(World world, SmartFluidTankBehaviour tank, ItemStack template) {
        if (!isPhotograph(template)) {
            return Optional.empty();
        }
        NbtCompound tag = template.getNbt();
        if (!hasFrame(tag)) {
            return Optional.of(DataResult.error(() -> "gui.councilcraft.printer.photograph.invalid"));
        }
        int generation = tag.getInt("photograph_generation");
        if (generation >= 2) {
            return Optional.of(DataResult.error(() -> "gui.councilcraft.printer.photograph.generation"));
        }
        NbtCompound out = tag.copy();
        out.putInt("photograph_generation", generation + 1);
        ItemStack result = new ItemStack(template.getItem());
        result.setNbt(out);
        return Optional.of(DataResult.success(new PhotoPrintingBehaviour(tank, result, isColor(tag), photographer(tag))));
    }

    @Override
    public boolean isValid() {
        return PrintFluids.isValid(this.tank, this.color, PrintPricing.PHOTO_MB);
    }

    @Override
    public boolean isSafeNBT() {
        return false;
    }

    @Override
    public int getRequiredItemCount(World world, ItemStack stack) {
        return stack.isIn(PHOTO_PAPERS) ? 1 : 0;
    }

    @Override
    public long getRequiredFluidAmount(World world, ItemStack stack, FluidStack fluid) {
        return PrintFluids.droplets(fluid, this.color, PrintPricing.PHOTO_MB);
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
        PrintFluids.goggles(tooltip, this.tank, this.result.getName(), this.author, this.color, PrintPricing.PHOTO_MB);
        return true;
    }
}
