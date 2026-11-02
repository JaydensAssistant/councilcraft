package dev.councilcraft.mixin;

import dev.councilcraft.cart.CartLeavesRules;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CC-174. Nifty carts phase through leaves like ridden mounts do under Leaf Me
 * Alone. That mod has no entity-list config (only {@code hasControllingPassenger}),
 * and pulled carts are not controlling-passenger mounts, so this mixin is required.
 *
 * <p>Targets {@link AbstractBlock#getCollisionShape} (the real owner in 1.20.1).
 * Leaf Me Alone's LeavesBlock override calls {@code super} for non-mounts, so this
 * inject still runs for carts. Collision is resolved on server and client.
 */
@Mixin(AbstractBlock.class)
public abstract class CartLeavesPassMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void councilcraft$cartsPassLeaves(
        BlockState state,
        BlockView world,
        BlockPos pos,
        ShapeContext context,
        CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!((Object) this instanceof LeavesBlock)) {
            return;
        }
        if (!(context instanceof EntityShapeContext entityContext)) {
            return;
        }
        Entity entity = entityContext.getEntity();
        if (entity == null) {
            return;
        }
        String id = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        if (CartLeavesRules.passesThroughLeaves(id)) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }
}
