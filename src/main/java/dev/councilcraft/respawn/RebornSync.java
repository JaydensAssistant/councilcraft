package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * S80. Tells the client when Reborn ends, in real time.
 * First long: expiry, epoch millis, or 0 when it is not active.
 * Second long: the server's clock at send, so the client can follow
 * real time without trusting its own wall clock.
 */
public final class RebornSync {
    public static final Identifier CHANNEL = new Identifier(CouncilCraftRules.MOD_ID, CouncilCraftRules.REBORN_CHANNEL);

    private RebornSync() {
    }

    public static void send(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(Reborn.until(player));
        buf.writeLong(System.currentTimeMillis());
        ServerPlayNetworking.send(player, CHANNEL, buf);
    }
}
