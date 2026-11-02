package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.councilcraft.rules.RiderChance;
import io.github.flemmli97.improvedmobs.events.EventCalls;
import net.minecraft.entity.mob.MobEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * CC-173A. Replace Improved Mobs' flat flyAIChance / guardianAIChance (Phantom Chance /
 * Guardian Chance) reads in {@link EventCalls#onEntityLoad} with {@link RiderChance} by
 * distance from 0,0. Soft-depend: applied only when improvedmobs is loaded.
 */
@Pseudo
@Mixin(value = EventCalls.class, remap = false)
public abstract class RiderChanceMixin {
    @ModifyExpressionValue(
        method = "onEntityLoad",
        at = @At(
            value = "FIELD",
            target = "Lio/github/flemmli97/improvedmobs/config/Config$CommonConfig;flyAIChance:F",
            opcode = Opcodes.GETSTATIC
        ),
        remap = false
    )
    private static float councilcraft$phantomChance(float original, @Local(argsOnly = true) MobEntity mob) {
        return (float) RiderChance.chance(mob.getX(), mob.getZ());
    }

    @ModifyExpressionValue(
        method = "onEntityLoad",
        at = @At(
            value = "FIELD",
            target = "Lio/github/flemmli97/improvedmobs/config/Config$CommonConfig;guardianAIChance:F",
            opcode = Opcodes.GETSTATIC
        ),
        remap = false
    )
    private static float councilcraft$guardianChance(float original, @Local(argsOnly = true) MobEntity mob) {
        return (float) RiderChance.chance(mob.getX(), mob.getZ());
    }
}
