package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * PT41 amendment 3. Reborn's activity test (S254/S256), run for EVERY player so KubeJS can skip AFK players
 * (councilcraft_tips.js). Same inputs as Reborn: the same hooks call {@link #selfMoved}, {@link #look} and
 * {@link #note} first, then Reborn's own code runs unchanged. This class keeps its own window and last-move tick
 * per player and never reads or writes Reborn's minutes, windows or growth.
 *
 * <p>At each five-minute window boundary the verdict is {@link RebornMotion#credit} of {@link RebornWindow#counts()},
 * exactly the test that credits a Reborn window. A credited window sets the command tag {@value #TAG}; a missed
 * one removes it. So {@code player.tags.contains('cc_active')} means "active in the last finished window". Join and
 * disconnect clear the tag, so a new session starts as not active until its first window has been judged.
 */
public final class PlayerActivity {
    /** Command (scoreboard) tag that KubeJS reads. */
    public static final String TAG = "cc_active";

    /** PT57: keyed by UUID across respawns. */
    private static final Map<UUID, Clock> CLOCKS = new HashMap<>();

    private PlayerActivity() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CLOCKS.remove(handler.player.getUuid());
            handler.player.removeScoreboardTag(TAG);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CLOCKS.remove(handler.player.getUuid());
            handler.player.removeScoreboardTag(TAG);
        });
        ServerTickEvents.END_SERVER_TICK.register(PlayerActivity::tick);
    }

    /** A respawn makes a new player object; carry the open window and last move over, as Reborn does for its own maps. */
    public static void copy(ServerPlayerEntity from, ServerPlayerEntity to) {
        // Same UUID on respawn — clock already under to.getUuid().
        if (from.getCommandTags().contains(TAG)) {
            to.addCommandTag(TAG);
        }
    }

    public static void selfMoved(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) {
            return;
        }
        clock(player).selfMoved(player.getServer().getTicks());
    }

    public static void look(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        clock(player).window.look(player.getYaw(), player.getPitch());
    }

    public static void note(ServerPlayerEntity player, RebornWindow.Action action, int target) {
        if (player == null || player.getServer() == null) {
            return;
        }
        clock(player).window.act(action, target, player.getServer().getTicks());
    }

    /** True when the last finished window passed Reborn's activity test. */
    public static boolean active(ServerPlayerEntity player) {
        return player.getCommandTags().contains(TAG);
    }

    private static Clock clock(ServerPlayerEntity player) {
        return CLOCKS.computeIfAbsent(player.getUuid(), ignored -> new Clock());
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTicks();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Boolean verdict = clock(player).tick(now);
            if (verdict == null) {
                continue;
            }
            if (verdict) {
                player.addCommandTag(TAG);
            } else {
                player.removeScoreboardTag(TAG);
            }
        }
    }

    /** One player's window, judged exactly like a Reborn window. Pure logic, so unit tests can drive it. */
    public static final class Clock {
        RebornWindow window = new RebornWindow();
        long lastMove = -1L;

        public void selfMoved(long tick) {
            this.lastMove = tick;
        }

        public RebornWindow window() {
            return this.window;
        }

        /** One server tick. Null mid-window; at a boundary, whether the window counted (then a new window opens). */
        public Boolean tick(long now) {
            if (!this.window.advance()) {
                return null;
            }
            boolean credited = RebornMotion.credit(this.window.counts(), now, this.lastMove);
            this.window = new RebornWindow();
            return credited;
        }

        /** Ticks per window, the same constant Reborn uses. */
        public static long windowTicks() {
            return CouncilCraftRules.REBORN_WINDOW_TICKS;
        }
    }
}
