package dev.councilcraft.supporter;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.councilcraft.rules.SupporterRules;
import dev.councilcraft.supporter.SupporterState.Assignment;
import dev.councilcraft.supporter.SupporterState.Donor;
import dev.councilcraft.supporter.SupporterState.Role;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/**
 * PPM102. Donor badges and government roles, server side only (players install nothing). A ★ badge in the tier
 * color, then the role prefix, then the name in chat and the tab list, and a "Supporters" tab-list footer.
 * Op-only /ccdonor and /ccrole; data saved with the world. Cosmetic only. PPM107: the name (and prefix) is the
 * role color, else gold for $20 donors, else soft blue for everyone; $15 and $20 stars are bold. PPM108:
 * {@code /ccrole create <Name> <color> ["<prefix>"]}, ids case-insensitive.
 */
public final class Supporters {
    private static volatile SupporterState state;

    private Supporters() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> state = server.getOverworld().getPersistentStateManager()
            .getOrCreate(SupporterState::fromNbt, SupporterState::new, SupporterState.ID));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> state = null);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SupporterState s = state;
            if (s != null) {
                s.updateName(handler.player.getUuid(), handler.player.getGameProfile().getName());
            }
            refresh(server);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> registerCommands(dispatcher));
    }

    /**
     * The decorated name for a player (badge, prefix, colored name), or null before the supporter data is loaded
     * (the name is then untouched). Every player gets at least the default name color (PPM107). Called from the
     * display-name and tab-list-name mixins.
     */
    @Nullable
    public static Text decorate(UUID uuid, Text name) {
        SupporterState s = state;
        if (s == null) {
            return null;
        }
        Donor donor = s.donors.get(uuid);
        Role role = s.roleOf(uuid);
        MutableText out = Text.empty();
        if (donor != null) {
            out.append(badge(donor.tier(), SupporterRules.BADGE)).append(Text.literal(" "));
        }
        if (role != null && !role.prefix().isEmpty()) {
            out.append(colored(role.prefix() + " ", role.color()));
        }
        int color = SupporterRules.nameColor(role == null ? null : role.color(), donor == null ? null : donor.tier());
        return out.append(name.copy().styled(style -> style.withColor(TextColor.fromRgb(color))));
    }

    private static MutableText colored(String text, int rgb) {
        return Text.literal(text).styled(style -> style.withColor(TextColor.fromRgb(rgb)));
    }

    /** Text in the tier color, bold for $15 and $20. */
    private static MutableText badge(int tier, String text) {
        return colored(text, SupporterRules.tierColor(tier)).styled(style -> style.withBold(SupporterRules.boldBadge(tier)));
    }

    /** "Supporters" in gold, then one line per tier, highest first: ★ and $ label in the tier color, gray names. */
    static Text footer(SupporterState s) {
        Map<String, Integer> tiers = new HashMap<>();
        s.donors.values().forEach(d -> tiers.put(d.name(), d.tier()));
        Map<Integer, List<String>> lines = SupporterRules.footerLines(tiers);
        if (lines.isEmpty()) {
            return Text.empty();
        }
        MutableText footer = Text.literal("Supporters").formatted(Formatting.GOLD);
        lines.forEach((tier, names) -> footer.append(Text.literal("\n"))
            .append(badge(tier, SupporterRules.BADGE + " $" + tier))
            .append(Text.literal("  " + String.join(", ", names)).formatted(Formatting.GRAY)));
        return footer;
    }

    /** Re-sends every online player's tab-list name and the footer to everyone. */
    static void refresh(MinecraftServer server) {
        SupporterState s = state;
        if (s == null) {
            return;
        }
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        server.getPlayerManager().sendToAll(
            new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME), players));
        server.getPlayerManager().sendToAll(new PlayerListHeaderS2CPacket(Text.empty(), footer(s)));
    }

    private static final SimpleCommandExceptionType NOT_TIER = new SimpleCommandExceptionType(
        Text.literal("Tier must be one of 2, 5, 10, 15, 20"));
    private static final SimpleCommandExceptionType NO_STATE = new SimpleCommandExceptionType(
        Text.literal("Supporter data is not loaded yet"));

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ccdonor").requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("add")
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .then(CommandManager.argument("tier", IntegerArgumentType.integer(2, 20))
                        .suggests((ctx, b) -> CommandSource.suggestMatching(
                            SupporterRules.TIERS.stream().map(String::valueOf), b))
                        .executes(ctx -> donorAdd(ctx)))))
            .then(CommandManager.literal("remove")
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .then(CommandManager.argument("tier", IntegerArgumentType.integer(2, 20))
                        .suggests((ctx, b) -> CommandSource.suggestMatching(
                            SupporterRules.TIERS.stream().map(String::valueOf), b))
                        .executes(ctx -> donorRemove(ctx)))))
            .then(CommandManager.literal("list").executes(ctx -> donorList(ctx))));

        dispatcher.register(CommandManager.literal("ccrole").requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("create")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    // One greedy argument: Brigadier's word() does not accept the # of a hex color.
                    .then(CommandManager.argument("color_and_prefix", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> CommandSource.suggestMatching(SupporterRules.NAMED_COLORS.keySet(), b))
                        .executes(ctx -> roleCreate(ctx)))))
            .then(CommandManager.literal("assign")
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests((ctx, b) -> CommandSource.suggestMatching(roleIds(), b))
                        .executes(ctx -> roleAssign(ctx)))))
            .then(CommandManager.literal("remove")
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .executes(ctx -> roleRemove(ctx))))
            .then(CommandManager.literal("delete")
                .then(CommandManager.argument("id", StringArgumentType.word())
                    .suggests((ctx, b) -> CommandSource.suggestMatching(roleIds(), b))
                    .executes(ctx -> roleDelete(ctx))))
            .then(CommandManager.literal("list").executes(ctx -> roleList(ctx))));
    }

    private static Iterable<String> roleIds() {
        SupporterState s = state;
        return s == null ? List.of() : List.copyOf(s.roles.keySet());
    }

    private static SupporterState requireState() throws CommandSyntaxException {
        SupporterState s = state;
        if (s == null) {
            throw NO_STATE.create();
        }
        return s;
    }

    private static void changed(CommandContext<ServerCommandSource> ctx, SupporterState s, String message) {
        s.markDirty();
        refresh(ctx.getSource().getServer());
        ctx.getSource().sendFeedback(() -> Text.literal(message), true);
    }

    private static void fail(CommandContext<ServerCommandSource> ctx, String message) {
        ctx.getSource().sendError(Text.literal(message));
    }

    private static int donorAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        SupporterState s = requireState();
        int tier = IntegerArgumentType.getInteger(ctx, "tier");
        if (!SupporterRules.isTier(tier)) {
            throw NOT_TIER.create();
        }
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(ctx, "player");
        for (GameProfile p : profiles) {
            s.donors.put(p.getId(), new Donor(p.getName(), tier));
            changed(ctx, s, p.getName() + " is now a $" + tier + " supporter");
        }
        return profiles.size();
    }

    private static int donorRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        SupporterState s = requireState();
        int tier = IntegerArgumentType.getInteger(ctx, "tier");
        if (!SupporterRules.isTier(tier)) {
            throw NOT_TIER.create();
        }
        int removed = 0;
        for (GameProfile p : GameProfileArgumentType.getProfileArgument(ctx, "player")) {
            Donor d = s.donors.get(p.getId());
            if (d == null) {
                fail(ctx, p.getName() + " is not a supporter");
            } else if (d.tier() != tier) {
                fail(ctx, p.getName() + " is a $" + d.tier() + " supporter, not $" + tier);
            } else {
                s.donors.remove(p.getId());
                changed(ctx, s, p.getName() + " is no longer a $" + tier + " supporter");
                removed++;
            }
        }
        return removed;
    }

    private static int donorList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        SupporterState s = requireState();
        Text footer = footer(s);
        if (s.donors.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("No supporters yet"), false);
        } else {
            ctx.getSource().sendFeedback(() -> footer, false);
        }
        return s.donors.size();
    }

    private static int roleCreate(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        SupporterState s = requireState();
        SupporterRules.RoleSpec spec;
        try {
            spec = SupporterRules.parseRoleSpec(StringArgumentType.getString(ctx, "name"),
                StringArgumentType.getString(ctx, "color_and_prefix"));
        } catch (IllegalArgumentException e) {
            fail(ctx, e.getMessage());
            return 0;
        }
        String id = spec.id();
        boolean existed = s.roles.containsKey(id);
        s.roles.put(id, new Role(spec.color(), spec.prefix()));
        changed(ctx, s, "Role " + id + (existed ? " updated: " : " created: ") + SupporterRules.hex(spec.color())
            + (spec.prefix().isEmpty() ? ", no prefix" : ", prefix " + spec.prefix()));
        return 1;
    }

    private static int roleAssign(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        SupporterState s = requireState();
        String id = SupporterRules.roleId(StringArgumentType.getString(ctx, "id"));
        if (!s.roles.containsKey(id)) {
            fail(ctx, "No role " + id + " (see /ccrole list)");
            return 0;
        }
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(ctx, "player");
        for (GameProfile p : profiles) {
            s.assignments.put(p.getId(), new Assignment(p.getName(), id));
            changed(ctx, s, p.getName() + " now has role " + id);
        }
        return profiles.size();
    }

    private static int roleRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        SupporterState s = requireState();
        int removed = 0;
        for (GameProfile p : GameProfileArgumentType.getProfileArgument(ctx, "player")) {
            Assignment a = s.assignments.remove(p.getId());
            if (a == null) {
                fail(ctx, p.getName() + " has no role");
            } else {
                changed(ctx, s, p.getName() + " no longer has role " + a.roleId());
                removed++;
            }
        }
        return removed;
    }

    private static int roleDelete(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        SupporterState s = requireState();
        String id = SupporterRules.roleId(StringArgumentType.getString(ctx, "id"));
        if (s.roles.remove(id) == null) {
            fail(ctx, "No role " + id);
            return 0;
        }
        s.assignments.values().removeIf(a -> a.roleId().equals(id));
        changed(ctx, s, "Role " + id + " deleted and unassigned");
        return 1;
    }

    private static int roleList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        SupporterState s = requireState();
        if (s.roles.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("No roles yet"), false);
            return 0;
        }
        MutableText out = Text.literal("Roles:");
        s.roles.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            String id = e.getKey();
            Role r = e.getValue();
            List<String> holders = s.assignments.values().stream().filter(a -> a.roleId().equals(id))
                .map(Assignment::name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
            out.append(Text.literal("\n" + id + " "))
                .append(colored(SupporterRules.hex(r.color()) + (r.prefix().isEmpty() ? "" : " " + r.prefix()), r.color()))
                .append(Text.literal(holders.isEmpty() ? " (nobody)" : ": " + String.join(", ", holders))
                    .formatted(Formatting.GRAY));
        });
        ctx.getSource().sendFeedback(() -> out, false);
        return s.roles.size();
    }
}
