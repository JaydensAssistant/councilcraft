package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.FeedingRules;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * PM24a / PM24b. Another player right-clicks a Reborn player (not full) with food. The feeder holds
 * the food forward (the BLOCK use pose, client side) for the normal eating time. If both stay within
 * 3 blocks and neither moves a block from where it started, the food goes to the Reborn player, who
 * also gets Growth Spurt (FeedingRules). The eating sound and crumbs play at the Reborn player.
 *
 * <p>PPM34. Every food gives Growth Spurt minutes, and bad food still applies its vanilla effects to the
 * baby. A baby can be fed again only once its current Growth Spurt has run out, and only when it could
 * eat that food itself (not full, unless the food is edible while full).
 */
public final class RebornFeeding {
    public static final TagKey<Item> MEALS = TagKey.of(RegistryKeys.ITEM, new Identifier(CouncilCraftRules.MOD_ID, "reborn_feed_meals"));
    /** UUIDs of Reborn players, for every client (who may be fed). */
    public static final Identifier REBORN_PLAYERS = new Identifier(CouncilCraftRules.MOD_ID, "reborn_players");
    /** Entity id of a feeder and whether it is feeding now, for the use pose and effects. */
    public static final Identifier FEEDING = new Identifier(CouncilCraftRules.MOD_ID, "feeding");
    /** PPM67. Action-bar line when the baby is still in a Growth Spurt. */
    public static final String REFUSED_SPURT = "councilcraft.feed.refused.spurt";
    /** PT21. Action-bar line when the feeder is farther than FeedingRules.MAX_DISTANCE. */
    public static final String REFUSED_TOO_FAR = "councilcraft.feed.refused.too_far";
    /** PT21. Action-bar line when the baby is full (and the food is not edible while full). */
    public static final String REFUSED_FULL = "councilcraft.feed.refused.full";
    /** PT57. Action-bar line when the target is not Reborn (right-click with food). */
    public static final String REFUSED_NOT_REBORN = "councilcraft.feed.refused.not_reborn";

    private record Feed(ServerPlayerEntity target, Hand hand, Item item, Vec3d feederStart, Vec3d targetStart) {
    }

    /** PT57: keyed by feeder UUID across respawns. */
    private static final Map<UUID, Feed> FEEDS = new HashMap<>();
    /** Client side: entity ids of players feeding now (from FEEDING packets). */
    public static final Set<Integer> CLIENT_FEEDERS = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> lastSent = Set.of();
    private static boolean resend;

    private RebornFeeding() {
    }

    public static void register() {
        GrowthSpurtEffect.register();
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity feeder)
                || !(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }
            return start(feeder, target, hand);
        });
        ServerTickEvents.END_SERVER_TICK.register(RebornFeeding::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> resend = true);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FEEDS.remove(handler.player.getUuid());
            resend = true;
        });
    }

    /** Growth Spurt minutes this food gives a Reborn player; 0 means it is not food. */
    public static int minutes(ItemStack stack) {
        if (stack.isEmpty() || !stack.isFood()) {
            return 0;
        }
        FoodComponent food = stack.getItem().getFoodComponent();
        if (food == null) {
            return 0;
        }
        return FeedingRules.minutes(stack.isIn(MEALS), food.getHunger(), food.getSaturationModifier());
    }

    /** PPM34. One feed per Growth Spurt, and only food the baby could eat itself right now. */
    private static boolean canTake(ServerPlayerEntity target, ItemStack stack) {
        FoodComponent food = stack.getItem().getFoodComponent();
        StatusEffectInstance spurt = target.getStatusEffect(GrowthSpurtEffect.EFFECT);
        return food != null
            && FeedingRules.canFeedAgain(spurt == null ? 0 : spurt.getDuration())
            && target.canConsume(food.isAlwaysEdible());
    }

    /** Whether this living entity is feeding someone now. Server: the live feed; client: the synced set. */
    public static boolean isFeeding(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        if (entity.getWorld().isClient) {
            return CLIENT_FEEDERS.contains(entity.getId());
        }
        return entity instanceof ServerPlayerEntity player && FEEDS.containsKey(player.getUuid());
    }

    private static ActionResult start(ServerPlayerEntity feeder, ServerPlayerEntity target, Hand hand) {
        if (FEEDS.containsKey(feeder.getUuid())) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = feeder.getStackInHand(hand);
        if (feeder == target || feeder.isSpectator() || target.isSpectator() || minutes(stack) <= 0) {
            return ActionResult.PASS;
        }
        // PT57. Clear refusal when the target is not Reborn (food in hand, intentional feed attempt).
        if (!Reborn.running(target)) {
            feeder.sendMessage(Text.translatable(REFUSED_NOT_REBORN, target.getDisplayName()), true);
            return ActionResult.CONSUME;
        }
        StatusEffectInstance spurt = target.getStatusEffect(GrowthSpurtEffect.EFFECT);
        if (spurt != null && !FeedingRules.canFeedAgain(spurt.getDuration())) {
            // PPM67. Say why on the action bar, and consume the click so the feeder does not eat it.
            feeder.sendMessage(Text.translatable(REFUSED_SPURT, target.getDisplayName(),
                StatusEffectUtil.getDurationText(spurt, 1.0f)), true);
            return ActionResult.CONSUME;
        }
        // PT21. Too far or full: same treatment, an action-bar reason and the click is consumed.
        if (feeder.squaredDistanceTo(target) > FeedingRules.MAX_DISTANCE * FeedingRules.MAX_DISTANCE) {
            feeder.sendMessage(Text.translatable(REFUSED_TOO_FAR, target.getDisplayName()), true);
            return ActionResult.CONSUME;
        }
        if (!canTake(target, stack)) {
            feeder.sendMessage(Text.translatable(REFUSED_FULL, target.getDisplayName()), true);
            return ActionResult.CONSUME;
        }
        FEEDS.put(feeder.getUuid(), new Feed(target, hand, stack.getItem(), feeder.getPos(), target.getPos()));
        feeder.setCurrentHand(hand);
        broadcast(feeder, true);
        return ActionResult.SUCCESS;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Feed>> it = FEEDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Feed> entry = it.next();
            ServerPlayerEntity feeder = server.getPlayerManager().getPlayer(entry.getKey());
            Feed feed = entry.getValue();
            if (feeder == null) {
                it.remove();
                continue;
            }
            if (!stillValid(server, feeder, feed)) {
                it.remove();
                if (feeder.isUsingItem()) {
                    feeder.stopUsingItem();
                }
                broadcast(feeder, false);
                continue;
            }
            int left = feeder.getItemUseTimeLeft();
            ItemStack stack = feeder.getActiveItem();
            if (left % 4 == 0 && left <= stack.getMaxUseTime() - 7) {
                effects(feed.target(), stack, 5);
            }
        }
        if (server.getTicks() % 20 == 0 || resend) {
            syncRebornPlayers(server);
        }
    }

    private static boolean stillValid(MinecraftServer server, ServerPlayerEntity feeder, Feed feed) {
        ServerPlayerEntity target = feed.target();
        return !feeder.isRemoved() && !target.isRemoved() && target.isAlive() && feeder.isAlive()
            && server.getPlayerManager().getPlayer(target.getUuid()) == target
            && feeder.isUsingItem() && feeder.getActiveHand() == feed.hand()
            && feeder.getActiveItem().isOf(feed.item())
            && Reborn.running(target) && canTake(target, feeder.getActiveItem())
            && feeder.getWorld() == target.getWorld()
            && feeder.squaredDistanceTo(target) <= FeedingRules.MAX_DISTANCE * FeedingRules.MAX_DISTANCE
            && feeder.getPos().squaredDistanceTo(feed.feederStart()) < FeedingRules.MAX_DRIFT * FeedingRules.MAX_DRIFT
            && target.getPos().squaredDistanceTo(feed.targetStart()) < FeedingRules.MAX_DRIFT * FeedingRules.MAX_DRIFT;
    }

    /**
     * Called from LivingEntity.consumeItem in place of stack.finishUsing(world, feeder), while feeding.
     * Server: the Reborn player eats it and gets Growth Spurt; what is left (a bowl) stays in the feeder's
     * hand. Client: nothing (the server's inventory sync follows), so the feeder never eats it locally.
     */
    public static ItemStack finish(PlayerEntity feeder, ItemStack stack, java.util.function.BiFunction<ItemStack, LivingEntity, ItemStack> eat) {
        if (!(feeder instanceof ServerPlayerEntity serverFeeder)) {
            return stack;
        }
        Feed feed = FEEDS.remove(serverFeeder.getUuid());
        broadcast(serverFeeder, false);
        if (feed == null || !stillValid(serverFeeder.getServer(), serverFeeder, feed)) {
            return stack;
        }
        ServerPlayerEntity target = feed.target();
        int minutes = minutes(stack);
        effects(target, stack, 16);
        ItemStack result = eat.apply(stack, target);
        int ticks = FeedingRules.spurtTicks(minutes);
        target.setStatusEffect(new StatusEffectInstance(GrowthSpurtEffect.EFFECT, ticks, 0, false, false, true), serverFeeder);
        if (ticks > 0) {
            // PT42 Here Comes the Airplane: the feeder gave a Reborn friend a Growth Spurt.
            dev.councilcraft.advancement.Milestones.grant(serverFeeder, dev.councilcraft.advancement.MilestoneRules.HERE_COMES_THE_AIRPLANE);
        }
        return result;
    }

    /** Vanilla's eating sound and item crumbs (LivingEntity.spawnConsumptionEffects), at the Reborn player. */
    private static void effects(ServerPlayerEntity target, ItemStack stack, int particles) {
        if (!(target.getWorld() instanceof ServerWorld world) || stack.isEmpty()) {
            return;
        }
        Vec3d look = target.getRotationVec(1.0f);
        world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack),
            target.getX() + look.x * 0.4, target.getEyeY() - 0.1, target.getZ() + look.z * 0.4,
            particles, 0.1, 0.05, 0.1, 0.05);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), stack.getEatSound(), SoundCategory.PLAYERS,
            0.5f + 0.5f * target.getRandom().nextInt(2),
            (target.getRandom().nextFloat() - target.getRandom().nextFloat()) * 0.2f + 1.0f);
    }

    private static void broadcast(ServerPlayerEntity feeder, boolean on) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(feeder.getId());
        buf.writeBoolean(on);
        Set<ServerPlayerEntity> to = new HashSet<>(PlayerLookup.tracking(feeder));
        to.add(feeder);
        for (ServerPlayerEntity player : to) {
            ServerPlayNetworking.send(player, FEEDING, PacketByteBufs.copy(buf));
        }
    }

    private static void syncRebornPlayers(MinecraftServer server) {
        Set<UUID> now = new HashSet<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (Reborn.running(player)) {
                now.add(player.getUuid());
            }
        }
        if (!resend && now.equals(lastSent)) {
            return;
        }
        resend = false;
        lastSent = Set.copyOf(now);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(now.size());
        for (UUID id : now) {
            buf.writeUuid(id);
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, REBORN_PLAYERS, PacketByteBufs.copy(buf));
        }
    }
}
