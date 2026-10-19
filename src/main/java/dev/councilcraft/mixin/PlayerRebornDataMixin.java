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
 * S80. Accrued active minutes are stored on the player, so a restart or a
 * logout keeps the same progress. Time offline is not added. The old wall-clock
 * key and the old tick count are dropped on save and are not read.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerRebornDataMixin {
    private static final String RETIRED_WALL_CLOCK = "councilcraft:reborn_until";
    private static final String RETIRED_TICKS = "councilcraft:reborn_ticks";

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void councilcraft$writeReborn(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }
        nbt.remove(RETIRED_WALL_CLOCK);
        nbt.remove(RETIRED_TICKS);
        if (Reborn.running(player)) {
            nbt.putInt(CouncilCraftRules.REBORN_NBT, (int) Reborn.minutes(player));
        } else {
            nbt.remove(CouncilCraftRules.REBORN_NBT);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void councilcraft$readReborn(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }
        if (nbt.contains(CouncilCraftRules.REBORN_NBT)) {
            Reborn.load(player, nbt.getInt(CouncilCraftRules.REBORN_NBT));
        }
    }
}
