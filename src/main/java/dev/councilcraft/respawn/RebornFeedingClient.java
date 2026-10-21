package dev.councilcraft.respawn;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/**
 * Client half of PM24a / PM24b: knows who is Reborn (so right-clicking them with food is sent as an
 * entity interaction instead of eating), who is feeding (for the use pose), and adds the food tooltip.
 * The server decides everything; it starts the use and syncs it back.
 */
@Environment(EnvType.CLIENT)
public final class RebornFeedingClient {
    private static volatile Set<UUID> reborn = Set.of();

    private RebornFeedingClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RebornFeeding.REBORN_PLAYERS, (client, handler, buf, response) -> {
            int n = buf.readVarInt();
            Set<UUID> ids = new HashSet<>();
            for (int i = 0; i < n; i++) {
                ids.add(buf.readUuid());
            }
            client.execute(() -> reborn = Set.copyOf(ids));
        });
        ClientPlayNetworking.registerGlobalReceiver(RebornFeeding.FEEDING, (client, handler, buf, response) -> {
            int id = buf.readVarInt();
            boolean on = buf.readBoolean();
            client.execute(() -> {
                if (on) {
                    RebornFeeding.CLIENT_FEEDERS.add(id);
                } else {
                    RebornFeeding.CLIENT_FEEDERS.remove(id);
                }
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            reborn = Set.of();
            RebornFeeding.CLIENT_FEEDERS.clear();
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient || !(entity instanceof PlayerEntity target) || target == player
                || player.isSpectator() || !reborn.contains(target.getUuid())
                || RebornFeeding.minutes(player.getStackInHand(hand)) <= 0) {
                return ActionResult.PASS;
            }
            // SUCCESS sends the interaction to the server instead of eating; the server starts the use.
            return ActionResult.SUCCESS;
        });
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            int minutes = RebornFeeding.minutes(stack);
            if (minutes > 0) {
                lines.add(Text.translatable(
                    minutes == 1 ? "councilcraft.feed.tooltip.one" : "councilcraft.feed.tooltip", minutes
                ).formatted(Formatting.GRAY));
            }
        });
    }
}
