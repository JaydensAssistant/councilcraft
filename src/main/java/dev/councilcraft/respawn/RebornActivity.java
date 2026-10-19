package dev.councilcraft.respawn;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

/**
 * Server-side activity for Reborn. These hooks only record what already happened.
 * They do not change the action. Chat is never hooked, so it cannot count.
 * The window itself decides whether a run was a held button or a macro.
 */
public final class RebornActivity {
    private RebornActivity() {
    }

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) ->
            note(player, RebornWindow.Action.BREAK, pos.hashCode())
        );
        UseBlockCallback.EVENT.register(RebornActivity::useBlock);
        UseItemCallback.EVENT.register(RebornActivity::useItem);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            note(player, RebornWindow.Action.USE, entity.getId());
            return ActionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            note(player, RebornWindow.Action.ATTACK, entity.getId());
            return ActionResult.PASS;
        });
    }

    public static void craft(PlayerEntity player) {
        note(player, RebornWindow.Action.CRAFT, 0);
    }

    public static void trade(PlayerEntity player) {
        note(player, RebornWindow.Action.TRADE, 0);
    }

    private static ActionResult useBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.getBlockState(hit.getBlockPos()).getBlock() instanceof CakeBlock) {
            return ActionResult.PASS;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (eatsOrDrinks(stack)) {
            return ActionResult.PASS;
        }
        RebornWindow.Action action = stack.getItem() instanceof BlockItem
            ? RebornWindow.Action.PLACE
            : RebornWindow.Action.USE;
        note(player, action, hit.getBlockPos().hashCode());
        return ActionResult.PASS;
    }

    private static TypedActionResult<ItemStack> useItem(PlayerEntity player, World world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!eatsOrDrinks(stack)) {
            note(player, RebornWindow.Action.USE, itemTarget(stack));
        }
        return TypedActionResult.pass(stack);
    }

    private static boolean eatsOrDrinks(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        UseAction action = stack.getUseAction();
        return action == UseAction.EAT || action == UseAction.DRINK || stack.isFood();
    }

    private static void note(PlayerEntity player, RebornWindow.Action action, int target) {
        if (player instanceof ServerPlayerEntity server) {
            Reborn.note(server, action, target);
        }
    }

    private static int itemTarget(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return -1 - Registries.ITEM.getRawId(stack.getItem());
    }
}
