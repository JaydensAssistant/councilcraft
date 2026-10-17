package dev.councilcraft.handshake;

import dev.councilcraft.CouncilCraft;
import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.HandshakeRule;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.PacketByteBuf;
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
                handler.disconnect(Text.literal(
                    "This server requires the CouncilCraft mod. Install councilcraft (the same version as the server) and reconnect."
                ));
                return;
            }
            String clientVersion = buf.readString(64);
            String serverVersion = CouncilCraft.version();
            if (!HandshakeRule.versionsMatch(serverVersion, clientVersion)) {
                handler.disconnect(Text.literal(
                    "CouncilCraft version mismatch. Server has " + serverVersion + ", client has " + clientVersion + "."
                ));
            }
        });
    }

    public static void registerClient() {
        ClientLoginNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, listenerAdder) -> {
            PacketByteBuf response = PacketByteBufs.create();
            response.writeString(CouncilCraft.version());
            return CompletableFuture.completedFuture(response);
        });
    }
}
