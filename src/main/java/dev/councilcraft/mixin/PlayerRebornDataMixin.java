package dev.councilcraft.mixin;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.respawn.Reborn;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * S80. The reborn deadline is stored on the player, so a restart or a logout
 * keeps the same real-time end. The in-memory copy is what the timer reads.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerRebornDataMixin {
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void councilcraft$writeReborn(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }
        long until = Reborn.until(player);
        if (until > 0L) {
            nbt.putLong(CouncilCraftRules.REBORN_NBT, until);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void councilcraft$readReborn(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }
        if (nbt.contains(CouncilCraftRules.REBORN_NBT)) {
            Reborn.setUntil(player, nbt.getLong(CouncilCraftRules.REBORN_NBT));
        }
    }
}
