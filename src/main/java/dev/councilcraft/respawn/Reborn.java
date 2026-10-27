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
 * active play. Idle time and time offline do not count. The accrued minutes
 * are saved on the player. The six hours start when the player respawns, not at
 * the moment of death, and dying again starts them over. The player sees it
 * as the Reborn status effect. There is no boss bar and no chat message.
 *
 * <p>Milk, a totem, a beacon and {@code /effect clear} take the icon off.
 * The next tick puts it back while active time is still left.
 * An operator clears it with {@code /councilcraft reborn clear}, or prints the
 * credited minutes and the last window with {@code /councilcraft reborn status}.
 *
 * <p>The icon's duration counts down like any other effect. At each window
 * boundary this class writes the remaining active time again: five minutes
 * shorter when the window counted, the same remaining time when it did not,
 * which snaps a countdown back up. Respawn writes it too. JOIN does not.
 * While minutes remain, the written duration cannot hit zero before the next
 * boundary. There are no particles.
 */
public final class Reborn {
    private static final Map<ServerPlayerEntity, Long> MINUTES = new WeakHashMap<>();
    private static final Map<ServerPlayerEntity, RebornWindow> WINDOWS = new WeakHashMap<>();
    private static final Map<ServerPlayerEntity, WindowVerdict> LAST_WINDOW = new WeakHashMap<>();
    /** Server tick of the last walk, jump, swim, sneak or sprint. Not saved. */
    private static final Map<ServerPlayerEntity, Long> LAST_MOVE = new WeakHashMap<>();
    /** The effect packet has been sent since this player object joined. JOIN itself must not send it. */
    private static final Set<ServerPlayerEntity> EFFECT_SENT = Collections.newSetFromMap(new WeakHashMap<>());
    /** Effect removal that must wait until after JOIN, so the packet is not a raw registry id. */
    private static final Set<ServerPlayerEntity> EFFECT_CLEAR_AFTER_JOIN = Collections.newSetFromMap(new WeakHashMap<>());
    /** PM24a: ticks of the current window spent under Growth Spurt. Not saved, like the window. */
    private static final Map<ServerPlayerEntity, Long> GROWTH_TICKS = new WeakHashMap<>();

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

    public static JoinStep joinStep(boolean active, long activeMinutes) {
        if (!active) {
            return JoinStep.NONE;
        }
        if (activeMinutes >= CouncilCraftRules.REBORN_ACTIVE_MINUTES) {
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
        // PT41 amendment 3: the same activity test for every player, for the tips AFK skip.
        PlayerActivity.register();
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            copy(oldPlayer, newPlayer);
            PlayerActivity.copy(oldPlayer, newPlayer);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // alive is true for an end return. That is not a death, so the accrued time stays.
            if (!alive) {
                begin(newPlayer);
            } else if (running(newPlayer)) {
                sync(newPlayer);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncJoined(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Minutes already saved stay. The unfinished window does not.
            WINDOWS.remove(handler.player);
            GROWTH_TICKS.remove(handler.player);
            LAST_WINDOW.remove(handler.player);
            LAST_MOVE.remove(handler.player);
        });
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

    public static long minutes(ServerPlayerEntity player) {
        Long minutes = MINUTES.get(player);
        return minutes == null ? 0L : minutes;
    }

    public static boolean running(ServerPlayerEntity player) {
        return MINUTES.containsKey(player);
    }

    /** Reads the saved active-minute count. Negative means the key was absent. */
    public static void load(ServerPlayerEntity player, long activeMinutes) {
        if (activeMinutes < 0L) {
            MINUTES.remove(player);
            return;
        }
        MINUTES.put(player, activeMinutes);
    }

    public static void copy(ServerPlayerEntity from, ServerPlayerEntity to) {
        if (running(from)) {
            MINUTES.put(to, minutes(from));
        }
        if (LAST_MOVE.containsKey(from)) {
            LAST_MOVE.put(to, LAST_MOVE.get(from));
        }
    }

    /** A sneak, sprint, walk, jump or swim. Ignored unless this player is Reborn. */
    public static void selfMoved(ServerPlayerEntity player) {
        PlayerActivity.selfMoved(player);
        if (player == null || !running(player) || player.getServer() == null) {
            return;
        }
        LAST_MOVE.put(player, (long) player.getServer().getTicks());
    }

    public static void begin(ServerPlayerEntity player) {
        MINUTES.put(player, 0L);
        WINDOWS.put(player, new RebornWindow());
        GROWTH_TICKS.remove(player);
        LAST_WINDOW.remove(player);
        sync(player);
    }

    /** A look packet. Ignored unless this player is Reborn. */
    public static void sampleLook(ServerPlayerEntity player) {
        PlayerActivity.look(player);
        if (player == null || !running(player)) {
            return;
        }
        window(player).look(player.getYaw(), player.getPitch());
    }

    /** One qualifying, or not qualifying, thing the player just did. */
    public static void note(ServerPlayerEntity player, RebornWindow.Action action, int target) {
        PlayerActivity.note(player, action, target);
        if (player == null || !running(player) || player.getServer() == null) {
            return;
        }
        window(player).act(action, target, player.getServer().getTicks());
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
        switch (joinStep(running(player), minutes(player))) {
            case NONE -> {
            }
            case ATTRIBUTES_AND_CLOCK -> {
                ((RebornStatusEffect) RebornStatusEffect.EFFECT).apply(player, RebornStats.at(minutes(player)));
                RebornSync.send(player);
            }
            case DEFER_EFFECT_CLEAR -> {
                MINUTES.remove(player);
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
            long accrued = minutes(player);
            if (accrued >= CouncilCraftRules.REBORN_ACTIVE_MINUTES) {
                finish(player);
                continue;
            }
            RebornWindow window = window(player);
            if (player.hasStatusEffect(GrowthSpurtEffect.EFFECT)) {
                GROWTH_TICKS.merge(player, 1L, Long::sum);
            }
            boolean boundary = window.advance();
            if (boundary) {
                long now = server.getTicks();
                long lastMove = LAST_MOVE.containsKey(player) ? LAST_MOVE.get(player) : -1L;
                boolean credited = RebornMotion.credit(window.counts(), now, lastMove);
                LAST_WINDOW.put(player, credited ? WindowVerdict.CREDITED : WindowVerdict.MISSED);
                WINDOWS.put(player, new RebornWindow());
                long growth = GROWTH_TICKS.getOrDefault(player, 0L);
                GROWTH_TICKS.remove(player);
                if (credited) {
                    accrued = RebornProgress.credit(accrued, true);
                    // PM24a Growth Spurt: 3x for the part of the window it covered. AFK windows still add nothing.
                    long extra = dev.councilcraft.rules.FeedingRules.extraMinutes(
                        growth, CouncilCraftRules.REBORN_WINDOW_TICKS, CouncilCraftRules.REBORN_WINDOW_MINUTES);
                    accrued = Math.min(CouncilCraftRules.REBORN_ACTIVE_MINUTES, accrued + extra);
                    MINUTES.put(player, accrued);
                }
                if (accrued >= CouncilCraftRules.REBORN_ACTIVE_MINUTES) {
                    finish(player);
                    continue;
                }
            }
            boolean missing = !player.hasStatusEffect(RebornStatusEffect.EFFECT);
            // One effect packet at a window boundary, not every tick. The first tick
            // after JOIN still sends the icon, after registry remap, and a missing
            // icon (milk, totem, /effect clear) is put back at the remaining time.
            if (boundary || missing || !EFFECT_SENT.contains(player)) {
                sync(player);
            }
        }
    }

    /** Sends the effect. Not for JOIN: the packet's effect id is a raw registry id. */
    private static void sync(ServerPlayerEntity player) {
        long accrued = minutes(player);
        if (!running(player) || accrued >= CouncilCraftRules.REBORN_ACTIVE_MINUTES) {
            finish(player);
            return;
        }
        ((RebornStatusEffect) RebornStatusEffect.EFFECT).apply(player, RebornStats.at(accrued));
        int duration = (int) Math.min(Integer.MAX_VALUE, RebornProgress.displayTicks(accrued));
        player.setStatusEffect(new StatusEffectInstance(
            RebornStatusEffect.EFFECT,
            Math.max(1, duration),
            0,
            false,
            false,
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
        long minutes = minutes(player);
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
        MINUTES.remove(player);
        WINDOWS.remove(player);
        GROWTH_TICKS.remove(player);
        if (player.hasStatusEffect(GrowthSpurtEffect.EFFECT)) {
            player.removeStatusEffect(GrowthSpurtEffect.EFFECT);
        }
        EFFECT_SENT.remove(player);
        if (player.hasStatusEffect(RebornStatusEffect.EFFECT)) {
            player.removeStatusEffect(RebornStatusEffect.EFFECT);
        } else {
            ((RebornStatusEffect) RebornStatusEffect.EFFECT).clear(player);
        }
        RebornSync.send(player);
    }
}
