package dev.councilcraft.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code getJumpVelocity} is protected. A jump is that impulse, not every rise. */
@Mixin(LivingEntity.class)
public interface RebornJumpAccess {
    @Invoker("getJumpVelocity")
    float councilcraft$jumpVelocity();
}
