package dev.councilcraft.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM43. Players always respawn at world spawn. Every non-null personal spawn point is refused (beds,
 * respawn anchors and /spawnpoint, which now silently does nothing); sleeping still works. A spawn point
 * saved before this build is cleared when the player loads.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class RespawnLockdownMixin {
    @Shadow
    @Nullable
    public abstract BlockPos getSpawnPointPosition();

    @Shadow
    public abstract void setSpawnPoint(RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage);

    @Inject(method = "setSpawnPoint", at = @At("HEAD"), cancellable = true)
    private void councilcraft$noPersonalSpawn(RegistryKey<World> dimension, @Nullable BlockPos pos, float angle,
                                              boolean forced, boolean sendMessage, CallbackInfo ci) {
        if (pos != null) {
            ci.cancel();
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void councilcraft$clearSavedSpawn(NbtCompound nbt, CallbackInfo ci) {
        if (this.getSpawnPointPosition() != null) {
            this.setSpawnPoint(World.OVERWORLD, null, 0.0f, false, false);
        }
    }
}
