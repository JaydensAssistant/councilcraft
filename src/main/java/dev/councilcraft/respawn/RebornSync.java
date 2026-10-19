package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Tells the client how much active Reborn time has been credited.
 * First long: accrued ticks, or -1 when it is not active.
 * Second long: remaining active ticks at the moment of the send.
 * The client keeps those numbers as they are. It does not add wall-clock time.
 */
public final class RebornSync {
    public static final Identifier CHANNEL = new Identifier(CouncilCraftRules.MOD_ID, CouncilCraftRules.REBORN_CHANNEL);

    private RebornSync() {
    }

    public static void send(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        if (!Reborn.running(player)) {
            buf.writeLong(-1L);
            buf.writeLong(0L);
        } else {
            long accrued = Reborn.ticks(player);
            buf.writeLong(accrued);
            buf.writeLong(RebornProgress.remaining(accrued));
        }
        ServerPlayNetworking.send(player, CHANNEL, buf);
    }
}
