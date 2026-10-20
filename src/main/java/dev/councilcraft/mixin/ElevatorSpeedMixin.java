package dev.councilcraft.mixin;

import dev.councilcraft.CouncilCraftRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * PM38. Create 6.0.8.1's elevator pulley moves at {@code convertToLinear(getSpeed() * 2)} blocks per tick,
 * clamped to 1.99. The speed handed to {@code convertToLinear} is doubled again, so 256 RPM reaches the
 * 1.99 ceiling (about 40 blocks/s). The clamp, acceleration and floor snapping that follow are Create's.
 * maxRotationSpeed and every other kinetic block are unchanged. The constant 2.0f is not touched: it
 * appears a second time in the same method (the deceleration term).
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity", remap = false)
public abstract class ElevatorSpeedMixin {
    @ModifyArg(method = "getMovementSpeed", remap = false, at = @At(value = "INVOKE", remap = false,
        target = "Lcom/simibubi/create/content/contraptions/elevator/ElevatorPulleyBlockEntity;convertToLinear(F)F"))
    private float councilcraft$fasterElevator(float speed) {
        return speed * CouncilCraftRules.ELEVATOR_SPEED_MULTIPLIER;
    }
}
