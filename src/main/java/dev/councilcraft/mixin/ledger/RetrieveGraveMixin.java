package dev.councilcraft.mixin.ledger;

import com.mojang.authlib.GameProfile;
import dev.councilcraft.ledger.GraveRobLedger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.mgin.graves.Graves;
import me.mgin.graves.api.InventoriesApi;
import me.mgin.graves.block.entity.GraveBlockEntity;
import me.mgin.graves.block.utility.RetrieveGrave;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CC-155 / CC-156. Log non-owner grave loot (right-click and break both go through
 * {@link RetrieveGrave#retrieve}). Owner retrieval is not logged.
 */
@Pseudo
@Mixin(value = RetrieveGrave.class, remap = false)
public class RetrieveGraveMixin {
    @Inject(method = "retrieve", at = @At("HEAD"), remap = false, require = 0)
    private static void councilcraft$graveRob(PlayerEntity player, GraveBlockEntity graveEntity,
            World world, BlockPos pos, boolean dropOnPlayer, boolean destroyGrave,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayerEntity actor) || graveEntity == null || world.isClient) {
            return;
        }
        GameProfile owner = graveEntity.getGraveOwner();
        if (owner == null) {
            return;
        }
        UUID ownerId = owner.getId();
        String ownerName = owner.getName();
        List<ItemStack> items = new ArrayList<>();
        try {
            for (InventoriesApi api : Graves.inventories) {
                DefaultedList<ItemStack> inv = graveEntity.getInventory(api.getID());
                if (inv != null) {
                    items.addAll(inv);
                }
            }
            for (String modId : Graves.unloadedInventories) {
                DefaultedList<ItemStack> inv = graveEntity.getInventory(modId);
                if (inv != null) {
                    items.addAll(inv);
                }
            }
        } catch (Throwable ignored) {
            // Soft: never break retrieval if inventory read fails.
        }
        GraveRobLedger.logIfRob(actor, ownerId, ownerName, pos, world, items);
    }
}
