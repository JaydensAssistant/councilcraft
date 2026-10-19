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
 * S80 / S169. After a death, the new body is small and weak for six hours of
 * active play. Idle time and time offline do not count. The accrued ticks are
 * saved on the player. The six hours start when the player respawns, not at
 * the moment of death, and dying again starts them over. The player sees it
 * as the Reborn status effect. There is no boss bar and no chat message.
 *
 * <p>Milk, a totem, a beacon and {@code /effect clear} take the icon off.
 * The next tick puts it back while active time is still left.
 * An operator clears it with {@code /councilcraft reborn clear}, or prints the
 * credited minutes and the last window with {@code /councilcraft reborn status}.
 *
 * <p>The icon's duration is the remaining active time. Vanilla would count that
 * down every tick, which looks like time passing while the player is idle.
 * {@code RebornDurationMixin} leaves the duration where the server last wrote
 * it. This class writes it again only when a window is credited, five minutes
 * shorter. An idle window does not send another packet.
 */
public final class Reborn {
    private static final Map<ServerPlayerEntity, Long> TICKS = new WeakHashMap<>();
    private static final Map<ServerPlayerEntity, RebornWindow> WINDOWS = new WeakHashMap<>();
    private static final Map<ServerPlayerEntity, WindowVerdict> LAST_WINDOW = new WeakHashMap<>();
    /** The effect packet has been sent since this player object joined. JOIN itself must not send it. */
    private static final Set<ServerPlayerEntity> EFFECT_SENT = Collections.newSetFromMap(new WeakHashMap<>());
    /** Effect removal that must wait until after JOIN, so the packet is not a raw registry id. */
    private static final Set<ServerPlayerEntity> EFFECT_CLEAR_AFTER_JOIN = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * What a player joining may do. A status-effect packet carries a raw registry
     * id. At JOIN the client has not applied Fabric's registry remap yet, so that
     * id is decoded as a different effect. The clock packet is two longs.
     */
    public enum JoinStep {
        /** Not Reborn. Send nothing. */
        NONE,
        /** Apply the attributes and send the accrued ticks. The effect icon waits for a later tick. */
        ATTRIBUTES_AND_CLOCK,
        /** Already finished. Clear attributes, send an inactive clock, drop the icon next tick. */
        DEFER_EFFECT_CLEAR
    }

    public static JoinStep joinStep(boolean active, long activeTicks) {
        if (!active) {
            return JoinStep.NONE;
        }
        if (activeTicks >= CouncilCraftRules.REBORN_ACTIVE_TICKS) {
            return JoinStep.DEFER_EFFECT_CLEAR;
        }
        return JoinStep.ATTRIBUTES_AND_CLOCK;
    }

    /** What the last finished window did. Not saved; a restart starts at none. */
    public enum WindowVerdict {
        NONE,
        CREDITED,
        MISSED
    }

    private Reborn() {
    }

    public static void register() {
        RebornStatusEffect.register();
        RebornActivity.register();
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> copy(oldPlayer, newPlayer));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // alive is true for an end return. That is not a death, so the accrued time stays.
            if (!alive) {
                begin(newPlayer);
            } else if (running(newPlayer)) {
                sync(newPlayer);
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
                            ))))
                    .then(CommandManager.literal("status")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(ctx -> status(
                                ctx.getSource(),
                                EntityArgumentType.getPlayer(ctx, "player")
                            ))))))
        );
    }

    public static long ticks(ServerPlayerEntity player) {
        Long ticks = TICKS.get(player);
        return ticks == null ? 0L : ticks;
    }

    public static boolean running(ServerPlayerEntity player) {
        return TICKS.containsKey(player);
    }

    /** Reads the saved active-tick count. Negative means the key was absent. */
    public static void load(ServerPlayerEntity player, long activeTicks) {
        if (activeTicks < 0L) {
            TICKS.remove(player);
            return;
        }
        TICKS.put(player, activeTicks);
    }

    public static void copy(ServerPlayerEntity from, ServerPlayerEntity to) {
        if (running(from)) {
            TICKS.put(to, ticks(from));
        }
    }

    public static void begin(ServerPlayerEntity player) {
        TICKS.put(player, 0L);
        WINDOWS.put(player, new RebornWindow());
        LAST_WINDOW.remove(player);
        sync(player);
    }

    /** A look packet. Ignored unless this player is Reborn. */
    public static void sampleLook(ServerPlayerEntity player) {
        if (player == null || !running(player)) {
            return;
        }
        window(player).look(player.getYaw(), player.getPitch());
    }

    /** One qualifying, or not qualifying, thing the player just did. */
    public static void note(ServerPlayerEntity player, RebornWindow.Action action, int target) {
        if (player == null || !running(player) || player.getServer() == null) {
            return;
        }
        RebornWindow window = window(player);
        boolean still = window.still(player.getX(), player.getY(), player.getZ());
        window.act(action, target, player.getServer().getTicks(), still);
    }

    private static RebornWindow window(ServerPlayerEntity player) {
        return WINDOWS.computeIfAbsent(player, ignored -> new RebornWindow());
    }

    /**
     * JOIN is too early for {@link #sync}. That method sends the effect, and the
     * effect packet's id is a registry raw id. Attributes and the clock do not
     * carry one. The next tick sends the effect, after the client has remapped
     * registries. An already-finished timer clears attributes now and removes
     * the icon on that same later tick.
     */
    private static void syncJoined(ServerPlayerEntity player) {
        switch (joinStep(running(player), ticks(player))) {
            case NONE -> {
            }
            case ATTRIBUTES_AND_CLOCK -> {
                ((RebornStatusEffect) RebornStatusEffect.EFFECT).apply(player, RebornStats.at(ticks(player)));
                RebornSync.send(player);
            }
            case DEFER_EFFECT_CLEAR -> {
                TICKS.remove(player);
                WINDOWS.remove(player);
                ((RebornStatusEffect) RebornStatusEffect.EFFECT).clear(player);
                EFFECT_CLEAR_AFTER_JOIN.add(player);
                RebornSync.send(player);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (EFFECT_CLEAR_AFTER_JOIN.remove(player) && player.hasStatusEffect(RebornStatusEffect.EFFECT)) {
                player.removeStatusEffect(RebornStatusEffect.EFFECT);
            }
            if (!running(player)) {
                WINDOWS.remove(player);
                continue;
            }
            long accrued = ticks(player);
            if (accrued >= CouncilCraftRules.REBORN_ACTIVE_TICKS) {
                finish(player);
                continue;
            }
            RebornWindow window = window(player);
            boolean boundary = window.advance();
            boolean credited = false;
            if (boundary) {
                credited = window.counts();
                LAST_WINDOW.put(player, credited ? WindowVerdict.CREDITED : WindowVerdict.MISSED);
                WINDOWS.put(player, new RebornWindow());
                if (credited) {
                    accrued = RebornProgress.credit(accrued, true);
                    TICKS.put(player, accrued);
                }
                if (accrued >= CouncilCraftRules.REBORN_ACTIVE_TICKS) {
                    finish(player);
                    continue;
                }
            }
            boolean missing = !player.hasStatusEffect(RebornStatusEffect.EFFECT);
            // Refresh the frozen duration only when a window counted. The first tick
            // after JOIN still sends the icon, after registry remap, and a missing
            // icon (milk, totem, /effect clear) is put back without changing the time.
            if (credited || missing || !EFFECT_SENT.contains(player)) {
                sync(player);
            }
        }
    }

    /** Sends the effect. Not for JOIN: the packet's effect id is a raw registry id. */
    private static void sync(ServerPlayerEntity player) {
        long accrued = ticks(player);
        if (!running(player) || accrued >= CouncilCraftRules.REBORN_ACTIVE_TICKS) {
            finish(player);
            return;
        }
        ((RebornStatusEffect) RebornStatusEffect.EFFECT).apply(player, RebornStats.at(accrued));
        int duration = (int) Math.min(Integer.MAX_VALUE, RebornProgress.remaining(accrued));
        player.setStatusEffect(new StatusEffectInstance(
            RebornStatusEffect.EFFECT,
            Math.max(1, duration),
            0,
            false,
            true,
            true
        ), null);
        RebornSync.send(player);
        EFFECT_SENT.add(player);
    }

    private static int clear(ServerCommandSource source, ServerPlayerEntity player) {
        if (!running(player) && !player.hasStatusEffect(RebornStatusEffect.EFFECT)) {
            source.sendError(Text.translatable("command.councilcraft.reborn.absent", player.getDisplayName()));
            return 0;
        }
        finish(player);
        LAST_WINDOW.remove(player);
        source.sendFeedback(
            () -> Text.translatable("command.councilcraft.reborn.cleared", player.getDisplayName()),
            true
        );
        return 1;
    }

    private static int status(ServerCommandSource source, ServerPlayerEntity player) {
        if (!running(player)) {
            source.sendError(Text.translatable("command.councilcraft.reborn.absent", player.getDisplayName()));
            return 0;
        }
        long minutes = ticks(player) / (20L * 60L);
        WindowVerdict verdict = LAST_WINDOW.getOrDefault(player, WindowVerdict.NONE);
        String key = switch (verdict) {
            case CREDITED -> "command.councilcraft.reborn.window.credited";
            case MISSED -> "command.councilcraft.reborn.window.missed";
            case NONE -> "command.councilcraft.reborn.window.none";
        };
        source.sendFeedback(
            () -> Text.translatable(
                "command.councilcraft.reborn.status",
                player.getDisplayName(),
                minutes,
                Text.translatable(key)
            ),
            false
        );
        return 1;
    }

    private static void finish(ServerPlayerEntity player) {
        TICKS.remove(player);
        WINDOWS.remove(player);
        EFFECT_SENT.remove(player);
        if (player.hasStatusEffect(RebornStatusEffect.EFFECT)) {
            player.removeStatusEffect(RebornStatusEffect.EFFECT);
        } else {
            ((RebornStatusEffect) RebornStatusEffect.EFFECT).clear(player);
        }
        RebornSync.send(player);
    }
}
