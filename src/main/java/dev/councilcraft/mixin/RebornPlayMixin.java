package dev.councilcraft.mixin;

import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.respawn.RebornWindow;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Look samples come from the move packets the server already handles, including
 * more than one in a single tick, so a reversal is not flattened into one sample.
 * Opening the inventory is a menu action. Chat packets are not hooked.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class RebornPlayMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerMove", at = @At("TAIL"))
    private void councilcraft$look(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        Reborn.sampleLook(this.player);
    }

    @Inject(method = "onVehicleMove", at = @At("TAIL"))
    private void councilcraft$vehicleLook(VehicleMoveC2SPacket packet, CallbackInfo ci) {
        Reborn.sampleLook(this.player);
    }

    @Inject(method = "onClientCommand", at = @At("TAIL"))
    private void councilcraft$inventory(ClientCommandC2SPacket packet, CallbackInfo ci) {
        if (packet.getMode() == ClientCommandC2SPacket.Mode.OPEN_INVENTORY) {
            Reborn.note(this.player, RebornWindow.Action.MENU, 0);
        }
    }

    @Inject(method = "onClickSlot", at = @At("TAIL"))
    private void councilcraft$slot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        Reborn.note(this.player, RebornWindow.Action.MENU, 0);
    }

    @Inject(method = "onButtonClick", at = @At("TAIL"))
    private void councilcraft$button(ButtonClickC2SPacket packet, CallbackInfo ci) {
        Reborn.note(this.player, RebornWindow.Action.MENU, 0);
    }
}
