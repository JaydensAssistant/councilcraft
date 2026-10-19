package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * S80 / S169. After a death, the new body is small and weak for 24 real hours.
 * The deadline is an absolute clock time, saved on the player, so logging out
 * does not pause it. The 24 hours start when the player respawns, not at the
 * moment of death. The player sees it as the Reborn status effect.
 * There is no boss bar and no chat message.
 *
 * <p>Milk, a totem, a beacon and {@code /effect clear} take the icon off.
 * The next tick puts it back while the real timer is still running.
 * An operator clears it on purpose with {@code /councilcraft reborn clear}.
 */
public final class Reborn {
    private static final Map<ServerPlayerEntity, Long> UNTIL = new WeakHashMap<>();
    private static final Map<ServerPlayerEntity, Long> NEXT_REFRESH = new WeakHashMap<>();
    /** Effect removal that must wait until after JOIN, so the packet is not a raw registry id. */
    private static final Set<ServerPlayerEntity> EFFECT_CLEAR_AFTER_JOIN = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * What a player joining may do. A status-effect packet carries a raw registry
     * id. At JOIN the client has not applied Fabric's registry remap yet, so that
     * id is decoded as a different effect. The clock packet is two longs.
     */
    public enum JoinStep {
        /** No timer. Send nothing. */
        NONE,
        /** Apply the attributes and send the clock. The effect icon waits for a later tick. */
        ATTRIBUTES_AND_CLOCK,
        /** Timer already over. Clear attributes, send a finished clock, drop the icon next tick. */
        DEFER_EFFECT_CLEAR
    }

    public static JoinStep joinStep(long until, long now) {
        if (until <= 0L) {
            return JoinStep.NONE;
        }
        if (now >= until) {
            return JoinStep.DEFER_EFFECT_CLEAR;
        }
        return JoinStep.ATTRIBUTES_AND_CLOCK;
    }

    private Reborn() {
    }

    public static void register() {
        RebornStatusEffect.register();
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> copy(oldPlayer, newPlayer));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // alive is true for an end return. That is not a death, so the timer stays.
            if (!alive) {
                begin(newPlayer);
            } else if (running(newPlayer)) {
                sync(newPlayer, System.currentTimeMillis());
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncJoined(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(Reborn::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(CommandManager.literal("councilcraft")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("reborn")
                    .then(CommandManager.literal("clear")
                        .executes(ctx -> clear(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(ctx -> clear(
                                ctx.getSource(),
                                EntityArgumentType.getPlayer(ctx, "player")
                            ))))))
        );
    }

    public static long until(ServerPlayerEntity player) {
        Long until = UNTIL.get(player);
        return until == null ? 0L : until;
    }

    public static void setUntil(ServerPlayerEntity player, long until) {
        if (until <= 0L) {
            UNTIL.remove(player);
        } else {
            UNTIL.put(player, until);
        }
    }

    public static boolean running(ServerPlayerEntity player) {
        long until = until(player);
        return until > System.currentTimeMillis();
    }

    public static void copy(ServerPlayerEntity from, ServerPlayerEntity to) {
        setUntil(to, until(from));
    }

    public static void begin(ServerPlayerEntity player) {
        setUntil(player, System.currentTimeMillis() + CouncilCraftRules.REBORN_MILLIS);
        sync(player, System.currentTimeMillis());
    }

    /**
     * JOIN is too early for {@link #sync}. That method sends the effect, and the
     * effect packet's id is a registry raw id. Attributes and the clock do not
     * carry one. Leaving the refresh unset makes the next tick send the effect,
     * after the client has remapped registries. An already-finished timer clears
     * attributes now and removes the icon on that same later tick.
     */
    private static void syncJoined(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        switch (joinStep(until(player), now)) {
            case NONE -> {
            }
            case ATTRIBUTES_AND_CLOCK -> {
                long start = until(player) - CouncilCraftRules.REBORN_MILLIS;
                ((RebornStatusEffect) RebornStatusEffect.EFFECT).apply(player, RebornStats.at(now - start));
                RebornSync.send(player);
            }
            case DEFER_EFFECT_CLEAR -> {
                setUntil(player, 0L);
                NEXT_REFRESH.remove(player);
                ((RebornStatusEffect) RebornStatusEffect.EFFECT).clear(player);
                EFFECT_CLEAR_AFTER_JOIN.add(player);
                RebornSync.send(player);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (EFFECT_CLEAR_AFTER_JOIN.remove(player) && player.hasStatusEffect(RebornStatusEffect.EFFECT)) {
                player.removeStatusEffect(RebornStatusEffect.EFFECT);
            }
            long until = until(player);
            if (until == 0L) {
                continue;
            }
            if (now >= until) {
                finish(player);
                continue;
            }
            boolean missing = !player.hasStatusEffect(RebornStatusEffect.EFFECT);
            Long next = NEXT_REFRESH.get(player);
            if (missing || next == null || now >= next) {
                sync(player, now);
            }
        }
    }

    /** Sends the effect. Not for JOIN: the packet's effect id is a raw registry id. */
    private static void sync(ServerPlayerEntity player, long now) {
        long remaining = until(player) - now;
        if (remaining <= 0L) {
            finish(player);
            return;
        }
        long start = until(player) - CouncilCraftRules.REBORN_MILLIS;
        ((RebornStatusEffect) RebornStatusEffect.EFFECT).apply(player, RebornStats.at(now - start));
        int ticks = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (remaining + 49L) / 50L));
        player.setStatusEffect(new StatusEffectInstance(
            RebornStatusEffect.EFFECT,
            ticks,
            0,
            false,
            true,
            true
        ), null);
        RebornSync.send(player);
        NEXT_REFRESH.put(player, now + CouncilCraftRules.REBORN_REFRESH_MILLIS);
    }

    private static int clear(ServerCommandSource source, ServerPlayerEntity player) {
        if (!running(player) && !player.hasStatusEffect(RebornStatusEffect.EFFECT)) {
            source.sendError(Text.translatable("command.councilcraft.reborn.absent", player.getDisplayName()));
            return 0;
        }
        finish(player);
        source.sendFeedback(
            () -> Text.translatable("command.councilcraft.reborn.cleared", player.getDisplayName()),
            true
        );
        return 1;
    }

    private static void finish(ServerPlayerEntity player) {
        setUntil(player, 0L);
        NEXT_REFRESH.remove(player);
        if (player.hasStatusEffect(RebornStatusEffect.EFFECT)) {
            player.removeStatusEffect(RebornStatusEffect.EFFECT);
        } else {
            ((RebornStatusEffect) RebornStatusEffect.EFFECT).clear(player);
        }
        RebornSync.send(player);
    }
}
