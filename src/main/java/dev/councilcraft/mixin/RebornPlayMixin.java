package dev.councilcraft.mixin;

import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.respawn.RebornMotion;
import dev.councilcraft.respawn.RebornWindow;
import dev.councilcraft.rules.DepthStriderRule;
import java.util.Set;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
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

    @Shadow
    private Vec3d requestedTeleportPos;

    /**
     * Runs after vanilla has accepted or rejected the move. A rejected packet
     * leaves {@code requestedTeleportPos} set and must not refresh the backstop.
     * The displacement is judged against the previous accepted step after drag,
     * not against {@code ServerPlayerEntity.getVelocity}, which the server does
     * not keep in step with the client.
     */
    @Inject(method = "onPlayerMove", at = @At("TAIL"))
    private void councilcraft$selfMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        ServerPlayerEntity player = this.player;
        if (player.getServer() == null) {
            return;
        }
        if (player.hasVehicle() || this.requestedTeleportPos != null) {
            RebornMotion.reset(player);
            return;
        }
        long tick = player.getServer().getTicks();
        boolean onGround = packet.isOnGround();
        boolean glide = player.isFallFlying() || player.isUsingRiptide();
        if (RebornMotion.sample(
            player,
            tick,
            player.getX(),
            player.getY(),
            player.getZ(),
            onGround,
            drag(player, onGround),
            ((RebornJumpAccess) player).councilcraft$jumpVelocity(),
            glide
        )) {
            Reborn.selfMoved(player);
        }
    }

    private static double drag(ServerPlayerEntity player, boolean onGround) {
        boolean grace = player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE);
        int depth = DepthStriderRule.effectiveLevel(EnchantmentHelper.getDepthStrider(player), grace);
        double slip = 0.6;
        if (onGround && !player.isTouchingWater() && !player.isInLava()) {
            slip = player.getWorld().getBlockState(((RebornEntityAccess) player).councilcraft$velocityAffectingPos())
                .getBlock()
                .getSlipperiness();
        }
        return RebornMotion.drag(
            player.isTouchingWater(),
            player.isInLava(),
            onGround,
            player.isSprinting(),
            depth,
            grace,
            slip
        );
    }

    @Inject(method = "onPlayerMove", at = @At("TAIL"))
    private void councilcraft$look(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        Reborn.sampleLook(this.player);
    }

    @Inject(method = "onVehicleMove", at = @At("TAIL"))
    private void councilcraft$vehicleLook(VehicleMoveC2SPacket packet, CallbackInfo ci) {
        RebornMotion.reset(this.player);
        Reborn.sampleLook(this.player);
    }

    @Inject(method = "requestTeleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void councilcraft$teleport(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        Set<PositionFlag> flags,
        CallbackInfo ci
    ) {
        RebornMotion.reset(this.player);
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
