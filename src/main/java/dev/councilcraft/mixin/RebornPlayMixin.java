package dev.councilcraft.mixin;

import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.respawn.RebornMotion;
import dev.councilcraft.respawn.RebornStats;
import dev.councilcraft.respawn.RebornWindow;
import dev.councilcraft.rules.DepthStriderRule;
import java.util.Set;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
            move(player, onGround, glide)
        )) {
            Reborn.selfMoved(player);
        }
    }

    /**
     * Expectations for this packet. Size comes from the Reborn effect. Movement
     * speed already includes that effect's speed modifier. The fluid push is the
     * same vector {@code updateMovementInFluid} adds, times the motion scale,
     * because {@code move} scales the whole displacement.
     */
    private static RebornMotion.Move move(ServerPlayerEntity player, boolean onGround, boolean glide) {
        boolean water = player.isTouchingWater();
        boolean lava = player.isInLava();
        boolean grace = player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE);
        int depth = DepthStriderRule.effectiveLevel(EnchantmentHelper.getDepthStrider(player), grace);
        double slip = 0.6;
        if (onGround && !water && !lava) {
            slip = player.getWorld().getBlockState(((RebornEntityAccess) player).councilcraft$velocityAffectingPos())
                .getBlock()
                .getSlipperiness();
        }
        double scale = 1.0;
        if (Reborn.running(player)) {
            scale = RebornStats.at(Reborn.minutes(player)).scale;
        }
        double movement = player.getMovementSpeed();
        double step;
        if (water) {
            step = RebornMotion.scaledSwimStroke(movement, depth, onGround, scale);
        } else if (onGround && !lava) {
            step = RebornMotion.scaledGroundStep(movement, slip, scale);
        } else {
            step = RebornMotion.scaledTravelStep(scale);
        }
        Vec3d flow = fluidPush(player).multiply(scale);
        boolean bubble = water && inBubbleColumn(player);
        double swimUp = water ? RebornMotion.SWIM_UP * scale : 0.0;
        double sink = water && !player.isSprinting() && !player.hasNoGravity()
            ? RebornMotion.WATER_SINK * scale
            : 0.0;
        return new RebornMotion.Move(
            RebornMotion.drag(water, lava, onGround, player.isSprinting(), depth, grace, slip),
            water ? RebornMotion.WATER_VERTICAL_DRAG : 0.0,
            RebornMotion.scaledJump(((RebornJumpAccess) player).councilcraft$jumpVelocity(), scale),
            step,
            swimUp,
            sink,
            flow.x,
            flow.y,
            flow.z,
            water,
            bubble,
            glide
        );
    }

    /**
     * Velocity {@code updateMovementInFluid} adds for water and lava. The slow
     * push up to length 0.0045 is left out: it depends on client velocity, which
     * this server does not keep, and it is below one scaled step.
     */
    private static Vec3d fluidPush(ServerPlayerEntity player) {
        if (!player.isPushedByFluids() || player.isRegionUnloaded()) {
            return Vec3d.ZERO;
        }
        Vec3d push = Vec3d.ZERO;
        if (player.isTouchingWater()) {
            push = push.add(fluidPush(player, FluidTags.WATER, RebornMotion.WATER_PUSH));
        }
        if (player.isInLava()) {
            double speed = player.getWorld().getDimension().ultrawarm()
                ? RebornMotion.LAVA_PUSH
                : RebornMotion.LAVA_PUSH_COLD;
            push = push.add(fluidPush(player, FluidTags.LAVA, speed));
        }
        return push;
    }

    private static Vec3d fluidPush(ServerPlayerEntity player, TagKey<Fluid> tag, double speed) {
        Box box = player.getBoundingBox().contract(0.001);
        World world = player.getWorld();
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.ceil(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.ceil(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.ceil(box.maxZ);
        double highest = 0.0;
        Vec3d sum = Vec3d.ZERO;
        int count = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    FluidState fluid = world.getFluidState(pos.set(x, y, z));
                    if (!fluid.isIn(tag)) {
                        continue;
                    }
                    double surface = y + fluid.getHeight(world, pos);
                    if (surface < box.minY) {
                        continue;
                    }
                    highest = Math.max(surface - box.minY, highest);
                    Vec3d velocity = fluid.getVelocity(world, pos);
                    if (highest < 0.4) {
                        velocity = velocity.multiply(highest);
                    }
                    sum = sum.add(velocity);
                    count++;
                }
            }
        }
        if (count == 0 || sum.lengthSquared() == 0.0) {
            return Vec3d.ZERO;
        }
        return sum.multiply(speed / count);
    }

    private static boolean inBubbleColumn(ServerPlayerEntity player) {
        Box box = player.getBoundingBox();
        World world = player.getWorld();
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.ceil(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.ceil(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.ceil(box.maxZ);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    if (world.getBlockState(pos.set(x, y, z)).isOf(Blocks.BUBBLE_COLUMN)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
