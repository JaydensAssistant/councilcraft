package dev.councilcraft.print;

import com.simibubi.create.content.fluids.VirtualFluid;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * PT14. CouncilCraft process ink: pays for color photo and album prints. A Create virtual fluid
 * (no block), the same kind Create uses for potions and tea. Drawn with the vanilla water texture
 * tinted on the client (no new art). PT15 adds {@link #BUCKET}.
 */
public final class ProcessInk {
    public static final Identifier ID = new Identifier("councilcraft", "process_ink");
    public static final Identifier FLOWING_ID = new Identifier("councilcraft", "flowing_process_ink");
    public static final Identifier BUCKET_ID = new Identifier("councilcraft", "process_ink_bucket");
    public static final int TINT = 0x2B2A4C;
    public static VirtualFluid STILL;
    public static VirtualFluid FLOWING;
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
        SimpleFlowableFluid.Properties properties = new SimpleFlowableFluid.Properties(() -> STILL, () -> FLOWING);
        STILL = Registry.register(Registries.FLUID, ID, new Typed(properties, true));
        FLOWING = Registry.register(Registries.FLUID, FLOWING_ID, new Typed(properties, false));
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

    /** A Create virtual fluid that reports the process ink fluid type. */
    private static final class Typed extends VirtualFluid {
        Typed(SimpleFlowableFluid.Properties properties, boolean source) {
            super(properties, source);
        }

        // Overrides Porting Lib's FluidExtension.getFluidType(), which its FluidMixin adds to
        // Fluid at runtime (not visible to javac here, so no @Override).
        public FluidType getFluidType() {
            return TYPE;
        }

        /** PT15. Create's VirtualFluid has no bucket (air); process ink has one. */
        @Override
        public Item getBucketItem() {
            return BUCKET;
        }
    }

    public static boolean is(Fluid fluid) {
        return fluid == STILL || fluid == FLOWING;
    }
}
