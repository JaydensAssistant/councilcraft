package dev.councilcraft.print;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * PT15. Bucket of process ink. It is a {@link BucketItem} (and {@link ProcessInk}'s fluids name it as their bucket),
 * so Fabric's transfer API treats it like any modded fluid bucket: an empty bucket fills from a Create fluid tank,
 * basin, item drain or spout, and this bucket empties into them. Process ink is a virtual fluid with no block, so the
 * bucket never places fluid in the world. CEI's Printer has no bucket handling of its own; using this bucket on a
 * Printer pours 1000 mB into its ink tank when it fits.
 */
public final class ProcessInkBucketItem extends BucketItem {
    private static final String PRINTER_BLOCK_ENTITY =
        "plus.dragons.createenchantmentindustry.common.fluids.printer.PrinterBlockEntity";

    public ProcessInkBucketItem(Settings settings) {
        super(ProcessInk.STILL, settings);
    }

    /** No fluid block exists for process ink, so world placement always fails and the bucket is kept. */
    @Override
    public boolean placeFluid(@Nullable PlayerEntity player, World world, BlockPos pos, @Nullable BlockHitResult hit) {
        return false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null || !PRINTER_BLOCK_ENTITY.equals(be.getClass().getName())) {
            return ActionResult.PASS;
        }
        Storage<FluidVariant> tank = printerTank(world, pos, context.getSide());
        if (tank == null) {
            return ActionResult.PASS;
        }
        PlayerEntity player = context.getPlayer();
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = tank.insert(FluidVariant.of(ProcessInk.STILL), FluidConstants.BUCKET, transaction);
            if (inserted != FluidConstants.BUCKET) {
                return ActionResult.FAIL;
            }
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }
            transaction.commit();
        }
        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.emitGameEvent(player, GameEvent.FLUID_PLACE, pos);
        if (player != null) {
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            ItemStack emptied = ItemUsage.exchangeStack(context.getStack(), player, new ItemStack(Items.BUCKET));
            player.setStackInHand(context.getHand(), emptied);
        }
        return ActionResult.CONSUME;
    }

    /** The Printer exposes its tank on every side but the bottom. */
    @Nullable
    private static Storage<FluidVariant> printerTank(World world, BlockPos pos, Direction side) {
        Storage<FluidVariant> tank = FluidStorage.SIDED.find(world, pos, side);
        return tank != null ? tank : FluidStorage.SIDED.find(world, pos, Direction.UP);
    }
}
