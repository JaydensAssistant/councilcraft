package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

/**
 * Client only. The server sends the accrued active minutes. This class keeps
 * that number for the inventory tooltip. It does not add wall-clock time.
 * The icon itself counts down until the next window rewrite.
 * Dedicated servers do not load this class.
 */
@Environment(EnvType.CLIENT)
public final class RebornClient {
    private static long accrued = -1L;
    private static boolean synced;

    private RebornClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RebornSync.CHANNEL, (client, handler, buf, response) -> {
            long packetAccrued = buf.readLong();
            buf.readLong();
            client.execute(() -> accept(packetAccrued));
        });
    }

    static void accept(long packetAccrued) {
        accrued = packetAccrued;
        synced = true;
        if (packetAccrued < 0L) {
            clearLocal();
        }
    }

    /**
     * Live penalty for the hover. The synced accrued minutes win. Before they
     * arrive, the effect's displayed duration is the remaining active time.
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
     * Current penalty. Prefers the synced accrued minutes.
     * {@code durationTicks} is the fallback for the moment before the packet arrives.
     * The icon is counting down, so this is only a stand-in.
     */
    public static RebornStats stats(int durationTicks) {
        if (synced && accrued >= 0L) {
            return RebornStats.at(accrued);
        }
        long remainingMinutes = Math.max(0, durationTicks) / (60L * 20L);
        return RebornStats.at(Math.max(0L, CouncilCraftRules.REBORN_ACTIVE_MINUTES - remainingMinutes));
    }

    private static void clearLocal() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.hasStatusEffect(RebornStatusEffect.EFFECT)) {
            player.removeStatusEffect(RebornStatusEffect.EFFECT);
        }
    }
}
