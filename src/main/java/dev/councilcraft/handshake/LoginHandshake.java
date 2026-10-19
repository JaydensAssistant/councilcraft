package dev.councilcraft.handshake;

import dev.councilcraft.CouncilCraft;
import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.HandshakeRule;
import dev.councilcraft.rules.LoginRejection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerLoginNetworkHandler;
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
 * <p>The query callback runs while other mods' login queries are still open.
 * Disconnecting there makes the client report "Connection reset by peer" and
 * drop the disconnect packet. The reason is stored, and
 * {@link dev.councilcraft.mixin.LoginAcceptMixin} sends it at the start of
 * {@code acceptPlayer}, which Fabric reaches only after every login query has
 * been answered. No delay is inserted.
 */
public final class LoginHandshake {
    public static final Identifier CHANNEL = new Identifier(CouncilCraftRules.MOD_ID, CouncilCraftRules.HANDSHAKE_CHANNEL);
    private static final ConcurrentHashMap<ServerLoginNetworkHandler, LoginRejection> REJECTIONS = new ConcurrentHashMap<>();

    private LoginHandshake() {
    }

    public static void registerServer() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) ->
            sender.sendPacket(CHANNEL, PacketByteBufs.empty())
        );
        ServerLoginConnectionEvents.DISCONNECT.register((handler, server) -> REJECTIONS.remove(handler));
        ServerLoginNetworking.registerGlobalReceiver(CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!understood) {
                reject(handler, HandshakeRule.MISSING_MOD);
                return;
            }
            String clientVersion = buf.readString(64);
            String serverVersion = CouncilCraft.version();
            if (!HandshakeRule.versionsMatch(serverVersion, clientVersion)) {
                reject(handler, HandshakeRule.versionMismatch(serverVersion, clientVersion));
            }
        });
    }

    private static void reject(ServerLoginNetworkHandler handler, String message) {
        REJECTIONS.computeIfAbsent(handler, ignored -> new LoginRejection()).record(message);
    }

    /** Called at the start of accept-player. Null means the client may join. */
    public static String takeRejection(ServerLoginNetworkHandler handler) {
        LoginRejection rejection = REJECTIONS.remove(handler);
        return rejection == null ? null : rejection.take();
    }

    public static void registerClient() {
        ClientLoginNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, listenerAdder) -> {
            PacketByteBuf response = PacketByteBufs.create();
            response.writeString(CouncilCraft.version());
            return CompletableFuture.completedFuture(response);
        });
    }
}
