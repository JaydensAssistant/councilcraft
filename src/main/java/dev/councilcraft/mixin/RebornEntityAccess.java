package dev.councilcraft.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code getVelocityAffectingPos} is protected. Ice drag is the block it returns. */
@Mixin(Entity.class)
public interface RebornEntityAccess {
    @Invoker("getVelocityAffectingPos")
    BlockPos councilcraft$velocityAffectingPos();
}
