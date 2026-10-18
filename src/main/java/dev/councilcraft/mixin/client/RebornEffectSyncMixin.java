package dev.councilcraft.mixin.client;

import dev.councilcraft.respawn.RebornClient;
import dev.councilcraft.respawn.RebornIdentity;
import dev.councilcraft.respawn.RebornStatusEffect;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client only. Jayden: this is the reconnect correction. A status-effect
 * packet can be applied as a different effect that still carries Reborn's
 * remaining time. When the clock is already synced, a duration that matches
 * that clock is applied as Reborn. The lookup does not care which other
 * effect's name was on the icon.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class RebornEffectSyncMixin {
    @Redirect(
        method = "onEntityStatusEffect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/packet/s2c/play/EntityStatusEffectS2CPacket;getEffectId()Lnet/minecraft/entity/effect/StatusEffect;"
        ),
        require = 1
    )
    private StatusEffect councilcraft$rewriteMatchingDuration(EntityStatusEffectS2CPacket packet) {
        StatusEffect effect = packet.getEffectId();
        if (effect == RebornStatusEffect.EFFECT) {
            return effect;
        }
        if (RebornIdentity.matches(packet.getDuration(), RebornClient.expectedTicks())) {
            return RebornStatusEffect.EFFECT;
        }
        return effect;
    }
}
