package dev.councilcraft.mixin;

import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.respawn.RebornStats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * While Reborn is running, hunger exhaustion is multiplied. Vanilla already
 * ignores this call on the client and for invulnerable players, then forwards
 * the amount to {@code HungerManager.addExhaustion}. This is not a Hunger
 * status effect.
 */
@Mixin(PlayerEntity.class)
public class PlayerExhaustionMixin {
    @ModifyVariable(method = "addExhaustion(F)V", at = @At("HEAD"), argsOnly = true)
    private float councilcraft$rebornExhaustion(float exhaustion) {
        if (!((Object) this instanceof ServerPlayerEntity player) || !Reborn.running(player)) {
            return exhaustion;
        }
        float multiplier = RebornStats.at(Reborn.ticks(player)).hunger;
        return RebornStats.exhaustion(exhaustion, multiplier);
    }
}
