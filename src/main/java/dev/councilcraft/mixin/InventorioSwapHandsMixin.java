package dev.councilcraft.mixin;

import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM84. A client that sends the vanilla swap-hands action (Controlify's Square button and its
 * radial entry, or any client without Inventorio's key hook) gets Inventorio's own swap
 * ({@code PlayerInventoryAddon.swapItemsInHands}, the same call Inventorio's swap packet makes)
 * instead of the vanilla swap. The vanilla swap, read through Better Combat's offhand redirect,
 * overwrote the selected utility-belt item and emptied the hotbar slot.
 *
 * <p>Server side, so it covers every client. Applied only with Inventorio. Spectators keep the
 * vanilla path (which does nothing for them).
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class InventorioSwapHandsMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;updateLastActionTime()V",
            shift = At.Shift.AFTER),
        cancellable = true)
    private void councilcraft$inventorioSwapHands(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (packet.getAction() != PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND || this.player.isSpectator()) {
            return;
        }
        PlayerInventoryAddon addon = PlayerInventoryAddon.Companion.getInventoryAddon(this.player);
        if (addon == null) {
            return;
        }
        addon.swapItemsInHands();
        ci.cancel();
    }
}
