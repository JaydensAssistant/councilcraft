package dev.councilcraft.handshake;

import dev.councilcraft.CouncilCraft;
import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.HandshakeRule;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginSynchronizer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * S244 G5. CouncilCraft has to be on the client as well as the server, because
 * riptide, horse step height and the horse speed cap are predicted on the client.
 *
 * <p>Mod Whitelist in this pack runs in blacklist mode (S185), so it blocks named
 * cheat mods and does not require CouncilCraft. This login query is what refuses
 * a client that did not install the mod, and a client on a different version.
 * Fabric only delivers the response when the client registered the same channel.
 *
 * <p>The query callback runs on the Netty thread. {@code handler.disconnect} writes the
 * login-disconnect packet and then closes the channel on that same thread, so the close
 * can win and the client shows the generic "Disconnected". The refusal is posted to the
 * server thread, and the query future stays open until the channel has closed, so login
 * does not continue into the play phase.
 */
public final class LoginHandshake {
    public static final Identifier CHANNEL = new Identifier(CouncilCraftRules.MOD_ID, CouncilCraftRules.HANDSHAKE_CHANNEL);

    private LoginHandshake() {
    }

    public static void registerServer() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) ->
            sender.sendPacket(CHANNEL, PacketByteBufs.empty())
        );
        ServerLoginNetworking.registerGlobalReceiver(CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!understood) {
                reject(server, handler, synchronizer, HandshakeRule.MISSING_MOD);
                return;
            }
            String clientVersion = buf.readString(64);
            String serverVersion = CouncilCraft.version();
            if (!HandshakeRule.versionsMatch(serverVersion, clientVersion)) {
                reject(server, handler, synchronizer, HandshakeRule.versionMismatch(serverVersion, clientVersion));
            }
        });
    }

    /**
     * Leave the Netty thread before closing the channel, and hold the login query
     * until that close has finished so the player is not accepted in between.
     */
    private static void reject(
        MinecraftServer server,
        ServerLoginNetworkHandler handler,
        LoginSynchronizer synchronizer,
        String message
    ) {
        synchronizer.waitFor(server.submit(() -> handler.disconnect(Text.literal(message))));
    }

    public static void registerClient() {
        ClientLoginNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, listenerAdder) -> {
            PacketByteBuf response = PacketByteBufs.create();
            response.writeString(CouncilCraft.version());
            return CompletableFuture.completedFuture(response);
        });
    }
}
