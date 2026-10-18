package com.simibubi.create.content.processing.basin;

import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Compile-only stand-in for Create 6.0.8.1. {@code getInputInventory} returns
 * {@code SmartInventory} in that build. A wider or narrower return type would
 * make the mixin call the wrong method. Not packed.
 */
public class BasinBlockEntity extends BlockEntity {
    public BasinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public SmartInventory getInputInventory() {
        return null;
    }
}
