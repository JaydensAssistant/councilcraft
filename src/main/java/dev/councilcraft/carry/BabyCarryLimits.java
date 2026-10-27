package dev.councilcraft.carry;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.respawn.Reborn;
import dev.councilcraft.respawn.RebornProgress;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;

/**
 * PT71. What a Reborn baby can carry scales with growth. Runs in the same early phase as
 * {@link BabyCarry}, before Carry On. Kids-can't-carry-kids (PT22) stays first.
 */
public final class BabyCarryLimits {
    public static final String TOO_SMALL = "councilcraft.carry.too_small";
    private static final Identifier BEFORE_CARRY_ON = new Identifier(CouncilCraftRules.MOD_ID, "before_carry_on");

    private BabyCarryLimits() {
    }

    public static void register() {
        UseBlockCallback.EVENT.addPhaseOrdering(BEFORE_CARRY_ON, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.register(BEFORE_CARRY_ON, (player, world, hand, hit) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity carrier)) {
                return ActionResult.PASS;
            }
            if (!gate(carrier)) {
                return ActionResult.PASS;
            }
            float fraction = RebornProgress.fraction(Reborn.minutes(carrier));
            BlockEntity be = world.getBlockEntity(hit.getBlockPos());
            int level = blockLevel(be);
            if (allowed(fraction, level, false)) {
                return ActionResult.PASS;
            }
            carrier.sendMessage(Text.translatable(TOO_SMALL), true);
            return ActionResult.FAIL;
        });
        UseEntityCallback.EVENT.addPhaseOrdering(BEFORE_CARRY_ON, Event.DEFAULT_PHASE);
        UseEntityCallback.EVENT.register(BEFORE_CARRY_ON, (player, world, hand, entity, hit) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity carrier)) {
                return ActionResult.PASS;
            }
            if (entity instanceof PlayerEntity) {
                return ActionResult.PASS; // PT22 / PT56 handle players
            }
            if (!gate(carrier)) {
                return ActionResult.PASS;
            }
            float fraction = RebornProgress.fraction(Reborn.minutes(carrier));
            int level = entityLevel(entity);
            if (allowed(fraction, level, true)) {
                return ActionResult.PASS;
            }
            carrier.sendMessage(Text.translatable(TOO_SMALL), true);
            return ActionResult.FAIL;
        });
    }

    private static boolean gate(ServerPlayerEntity carrier) {
        if (!Reborn.running(carrier)) {
            return false;
        }
        if (!carrier.getMainHandStack().isEmpty() || !carrier.getOffHandStack().isEmpty()) {
            return false;
        }
        CarryOnData carry = CarryOnDataManager.getCarryData(carrier);
        return !carry.isCarrying() && carry.isKeyPressed();
    }

    /** Carry On's block weight: saved BE NBT length / 500, capped at 4. */
    public static int blockLevel(BlockEntity be) {
        if (be == null) {
            return 0;
        }
        NbtCompound nbt = be.createNbtWithId();
        int i = nbt.toString().length() / 500;
        return Math.min(4, i);
    }

    /** Carry On's entity weight: (int)(height * width), uncapped here (tiers cap at 1). */
    public static int entityLevel(Entity entity) {
        return (int) (entity.getHeight() * entity.getWidth());
    }

    /**
     * Growth tiers (PT71): below 1/3 → level-0 blocks only, no entities;
     * 1/3–2/3 → also level-0 entities; 2/3+ → up to level 1. Fully grown uses adult rules.
     */
    public static boolean allowed(float fraction, int level, boolean entity) {
        if (fraction >= 1.0f) {
            return true;
        }
        if (entity) {
            if (fraction < 1.0f / 3.0f) {
                return false;
            }
            if (fraction < 2.0f / 3.0f) {
                return level <= 0;
            }
            return level <= 1;
        }
        if (fraction < 2.0f / 3.0f) {
            return level <= 0;
        }
        return level <= 1;
    }
}
