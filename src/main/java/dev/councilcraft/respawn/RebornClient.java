package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

/**
 * Client only. Owner review: this is the clock the inventory tooltip uses.
 * The server sends the expiry and its own time. This class keeps those two
 * numbers and the client time they arrived, and turns them into {@link RebornStats}.
 * Dedicated servers do not load this class.
 */
@Environment(EnvType.CLIENT)
public final class RebornClient {
    private static long until;
    private static long serverNow;
    private static long clientNowAtSync;
    private static boolean synced;

    private RebornClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RebornSync.CHANNEL, (client, handler, buf, response) -> {
            long packetUntil = buf.readLong();
            long packetServerNow = buf.readLong();
            client.execute(() -> accept(packetUntil, packetServerNow));
        });
    }

    static void accept(long packetUntil, long packetServerNow) {
        until = packetUntil;
        serverNow = packetServerNow;
        clientNowAtSync = System.currentTimeMillis();
        synced = true;
        if (packetUntil <= 0L) {
            clearLocal();
        }
    }

    /**
     * Live penalty for the hover. The synced clock wins. Before it arrives,
     * the Reborn effect's own countdown is the remaining real time.
     */
    public static RebornStats current() {
        int ticks = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            StatusEffectInstance effect = client.player.getStatusEffect(RebornStatusEffect.EFFECT);
            if (effect != null) {
                ticks = effect.getDuration();
            }
        }
        return stats(ticks);
    }

    /**
     * Current penalty. Prefers the synced real-time expiry.
     * {@code durationTicks} is the fallback for the moment before the packet arrives:
     * the effect countdown is the remaining real time, in game ticks.
     */
    public static RebornStats stats(int durationTicks) {
        if (synced && until > 0L) {
            long elapsed = RebornClock.elapsed(until, serverNow, clientNowAtSync, System.currentTimeMillis());
            return RebornStats.at(elapsed);
        }
        long remaining = Math.max(0, durationTicks) * 50L;
        return RebornStats.at(CouncilCraftRules.REBORN_MILLIS - remaining);
    }

    private static void clearLocal() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.hasStatusEffect(RebornStatusEffect.EFFECT)) {
            player.removeStatusEffect(RebornStatusEffect.EFFECT);
        }
    }
}
