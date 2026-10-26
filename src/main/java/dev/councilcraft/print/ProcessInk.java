package dev.councilcraft.print;

import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * PT14. CouncilCraft process ink, shown as "CMYK Ink" (PT36): pays for color photo and album prints. PT15 adds
 * {@link #BUCKET}. PT36: a real placeable fluid (Registrate SimpleFlowableFluid source + flowing, and the
 * {@link #BLOCK} fluid block), drawn untinted with its own CMYK-band textures. Registry IDs are unchanged.
 */
public final class ProcessInk {
    public static final Identifier ID = new Identifier("councilcraft", "process_ink");
    public static final Identifier FLOWING_ID = new Identifier("councilcraft", "flowing_process_ink");
    public static final Identifier BUCKET_ID = new Identifier("councilcraft", "process_ink_bucket");
    public static SimpleFlowableFluid STILL;
    public static SimpleFlowableFluid FLOWING;
    /** PT36. The placed fluid block, councilcraft:process_ink. */
    public static FluidBlock BLOCK;
    /**
     * Porting Lib fluid type. CEI's printer renderer (and other Create-side code) calls
     * Fluid.getFluidType() and crashes on null, so every modded fluid must supply one; this is
     * the same pattern Create Dragons Plus uses for its dye fluids (TypedFlowableFluid).
     */
    public static FluidType TYPE;
    /** PT15. Registered right after the fluids; both fluids report it from getBucketItem(). */
    public static Item BUCKET;

    private ProcessInk() {
    }

    public static void register() {
        TYPE = Registry.register(PortingLibFluids.FLUID_TYPES, ID,
                new FluidType(FluidType.Properties.create().descriptionId("fluid.councilcraft.process_ink")));
        SimpleFlowableFluid.Properties properties = new SimpleFlowableFluid.Properties(() -> STILL, () -> FLOWING)
                .block(() -> BLOCK).bucket(() -> BUCKET);
        STILL = Registry.register(Registries.FLUID, ID, new Source(properties));
        FLOWING = Registry.register(Registries.FLUID, FLOWING_ID, new Flowing(properties));
        BLOCK = Registry.register(Registries.BLOCK, ID, new Block(STILL, AbstractBlock.Settings.copy(Blocks.WATER)));
        BUCKET = Registry.register(Registries.ITEM, BUCKET_ID,
                new ProcessInkBucketItem(new Item.Settings().recipeRemainder(Items.BUCKET).maxCount(1)));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.addAfter(Items.MILK_BUCKET, BUCKET));
        FluidVariantAttributeHandler name = new FluidVariantAttributeHandler() {
            @Override
            public Text getName(FluidVariant variant) {
                return Text.translatable("fluid.councilcraft.process_ink");
            }
        };
        FluidVariantAttributes.register(STILL, name);
        FluidVariantAttributes.register(FLOWING, name);
    }

    /** Source fluid that reports the process ink fluid type. */
    private static final class Source extends SimpleFlowableFluid.Source {
        Source(SimpleFlowableFluid.Properties properties) {
            super(properties);
        }

        // Overrides Porting Lib's FluidExtension.getFluidType(), which its FluidMixin adds to
        // Fluid at runtime (not visible to javac here, so no @Override).
        public FluidType getFluidType() {
            return TYPE;
        }
    }

    /** Flowing fluid that reports the process ink fluid type. */
    private static final class Flowing extends SimpleFlowableFluid.Flowing {
        Flowing(SimpleFlowableFluid.Properties properties) {
            super(properties);
        }

        public FluidType getFluidType() {
            return TYPE;
        }
    }

    /** PT36. The world block for placed ink (FluidBlock's constructor is protected in 1.20.1). */
    private static final class Block extends FluidBlock {
        Block(FlowableFluid fluid, AbstractBlock.Settings settings) {
            super(fluid, settings);
        }
    }

    public static boolean is(Fluid fluid) {
        return fluid == STILL || fluid == FLOWING;
    }
}
