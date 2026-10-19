package dev.councilcraft.mixin;

import net.minecraft.entity.mob.PatrolEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code setPatrolling} is protected. The fallback evoker uses it to clear the flag. */
@Mixin(PatrolEntity.class)
public interface PatrolEntityAccess {
    @Invoker("setPatrolling")
    void councilcraft$setPatrolling(boolean patrolling);
}
