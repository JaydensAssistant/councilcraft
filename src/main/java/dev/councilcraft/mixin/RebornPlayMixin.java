package dev.councilcraft.mixin;

import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.respawn.RebornMotion;
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

    @Inject(
        method = "onPlayerMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
            shift = At.Shift.AFTER
        )
    )
    private void councilcraft$selfMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        ServerPlayerEntity player = this.player;
        if (player.hasVehicle()) {
            return;
        }
        var velocity = player.getVelocity();
        double dx = packet.getX(player.getX()) - player.getX();
        double dy = packet.getY(player.getY()) - player.getY();
        double dz = packet.getZ(player.getZ()) - player.getZ();
        if (RebornMotion.positionIsSelfMove(false, dx, dy, dz, velocity.x, velocity.y, velocity.z)) {
            Reborn.selfMoved(player);
        }
    }

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
        ClientCommandC2SPacket.Mode mode = packet.getMode();
        if (mode == ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
            || mode == ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY
            || mode == ClientCommandC2SPacket.Mode.START_SPRINTING
            || mode == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
            Reborn.selfMoved(this.player);
        }
        if (mode == ClientCommandC2SPacket.Mode.OPEN_INVENTORY) {
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
