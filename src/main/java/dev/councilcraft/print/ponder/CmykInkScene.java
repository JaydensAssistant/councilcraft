package dev.councilcraft.print.ponder;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.councilcraft.print.ProcessInk;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * PT36 (d). "CMYK Ink": a mixer over a basin turns liquid black dye plus cyan, magenta and yellow dye into CMYK Ink
 * (the councilcraft_recipes.js mixing recipe); a pump and pipe carry it into a CEI Printer, which prints a color photo
 * copy; then a placed pool of the ink. Structure: assets/councilcraft/ponder/cmyk_ink.nbt (pack/tools/mkponder.py).
 */
public final class CmykInkScene {
    public static final String ID = "cmyk_ink";
    /** One batch of the mixing recipe: 250 mB in droplets (81 per mB). */
    private static final long BATCH = 20250L;

    private CmykInkScene() {
    }

    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(ID, "CMYK Ink");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.85F);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos basin = util.grid().at(1, 1, 3);
        BlockPos mixer = util.grid().at(1, 3, 3);
        BlockPos pump = util.grid().at(2, 1, 3);
        BlockPos depot = util.grid().at(3, 1, 2);
        BlockPos printer = util.grid().at(3, 3, 2);
        Selection pipes = util.select().position(pump).add(util.select().fromTo(3, 1, 3, 3, 3, 3));

        scene.world().showSection(util.select().position(basin).add(util.select().position(mixer)), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).attachKeyFrame().pointAt(util.vector().topOf(basin)).placeNearTarget()
                .text("Mix Liquid Black Dye with Cyan, Magenta and Yellow Dye to make CMYK Ink");
        Vec3d top = util.vector().topOf(basin);
        scene.overlay().showControls(top.add(-0.6, 0, 0), Pointing.DOWN, 50).withItem(new ItemStack(Items.CYAN_DYE));
        scene.overlay().showControls(top, Pointing.DOWN, 50).withItem(new ItemStack(Items.MAGENTA_DYE));
        scene.overlay().showControls(top.add(0.6, 0, 0), Pointing.DOWN, 50).withItem(new ItemStack(Items.YELLOW_DYE));
        Fluid black = Registries.FLUID.get(new Identifier("create_dragons_plus", "black_dye"));
        Fluid blackDye = black == Fluids.EMPTY ? Fluids.WATER : black;
        scene.world().modifyBlockEntity(basin, BasinBlockEntity.class,
                be -> be.getTanks().getFirst().getPrimaryHandler().setFluid(new FluidStack(blackDye, BATCH)));
        scene.world().createItemOnBeltLike(basin, Direction.UP, new ItemStack(Items.CYAN_DYE));
        scene.world().createItemOnBeltLike(basin, Direction.UP, new ItemStack(Items.MAGENTA_DYE));
        scene.world().createItemOnBeltLike(basin, Direction.UP, new ItemStack(Items.YELLOW_DYE));
        scene.idle(20);
        scene.world().setKineticSpeed(util.select().position(mixer), 32.0F);
        scene.world().modifyBlockEntity(mixer, MechanicalMixerBlockEntity.class, MechanicalMixerBlockEntity::startProcessingBasin);
        scene.idle(40);
        scene.world().modifyBlockEntity(basin, BasinBlockEntity.class, be -> {
            for (int slot = 0; slot < be.getInputInventory().size(); slot++) {
                be.getInputInventory().setStack(slot, ItemStack.EMPTY);
            }
            be.getTanks().getFirst().getPrimaryHandler().setFluid(FluidStack.EMPTY);
            be.getTanks().getSecond().getPrimaryHandler().setFluid(new FluidStack(ProcessInk.STILL, BATCH));
        });
        scene.world().setKineticSpeed(util.select().position(mixer), 0.0F);
        scene.idle(40);

        scene.world().showSection(pipes, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(depot).add(util.select().position(printer)), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).attachKeyFrame().pointAt(util.vector().centerOf(printer)).placeNearTarget()
                .text("Pump it into a Printer: CMYK Ink prints copies of color photographs");
        scene.world().setKineticSpeed(util.select().position(pump), 64.0F);
        scene.idle(20);
        scene.world().modifyBlockEntity(basin, BasinBlockEntity.class,
                be -> be.getTanks().getSecond().getPrimaryHandler().setFluid(FluidStack.EMPTY));
        scene.world().modifyBlockEntity(printer, SmartBlockEntity.class, be -> {
            SmartFluidTankBehaviour tank = be.getBehaviour(SmartFluidTankBehaviour.TYPE);
            if (tank != null) {
                tank.getPrimaryHandler().setFluid(new FluidStack(ProcessInk.STILL, BATCH));
            }
        });
        scene.idle(30);
        scene.world().modifyBlockEntity(depot, DepotBlockEntity.class, be -> be.setHeldItem(new ItemStack(Items.PAPER)));
        scene.idle(40);
        scene.world().modifyBlockEntity(depot, DepotBlockEntity.class, be -> be.setHeldItem(colorCopy()));
        scene.idle(60);

        BlockPos pool = util.grid().at(1, 1, 1);
        ElementLink<WorldSectionElement> poolSection =
                scene.world().showIndependentSection(util.select().fromTo(0, 1, 0, 2, 1, 1), Direction.DOWN);
        scene.idle(10);
        scene.world().setBlocks(util.select().fromTo(0, 1, 0, 1, 1, 1), ProcessInk.BLOCK.getDefaultState(), false);
        scene.overlay().showText(70).attachKeyFrame().pointAt(util.vector().topOf(pool)).placeNearTarget()
                .text("A Bucket of CMYK Ink can also be poured out and picked back up");
        scene.idle(80);
    }

    /** A printed color photograph, like the Printing recipe's output. */
    private static ItemStack colorCopy() {
        Item photograph = Registries.ITEM.get(new Identifier("exposure", "photograph"));
        ItemStack stack = new ItemStack(photograph == Items.AIR ? Items.PAPER : photograph);
        NbtCompound tag = stack.getOrCreateNbt();
        tag.putString("photograph_type", "color");
        tag.putInt("photograph_generation", 1);
        return stack;
    }
}
