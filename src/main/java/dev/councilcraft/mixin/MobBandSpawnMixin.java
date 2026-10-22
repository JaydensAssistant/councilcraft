package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.councilcraft.rules.MobBand;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PPM81 (1). Overworld monster spawn attempts pick their Y within {@link MobBand#BAND} blocks of the horizontally
 * nearest player instead of anywhere from the bottom to the surface. The X/Z pick is vanilla's. With no player the
 * attempt is vanilla; when the band holds no valid Y the attempt is skipped the way vanilla skips a bottom-row pick.
 */
@Mixin(SpawnHelper.class)
public abstract class MobBandSpawnMixin {
    @ModifyExpressionValue(
        method = "spawnEntitiesInChunk(Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/SpawnHelper$Checker;Lnet/minecraft/world/SpawnHelper$Runner;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SpawnHelper;getRandomPosInChunkSection(Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/WorldChunk;)Lnet/minecraft/util/math/BlockPos;"))
    private static BlockPos councilcraft$nearPlayerY(BlockPos pos, @Local(argsOnly = true) SpawnGroup group,
        @Local(argsOnly = true) ServerWorld world, @Local(argsOnly = true) WorldChunk chunk) {
        if (group != SpawnGroup.MONSTER || world.getRegistryKey() != World.OVERWORLD) {
            return pos;
        }
        PlayerEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            double dx = player.getX() - (pos.getX() + 0.5);
            double dz = player.getZ() - (pos.getZ() + 0.5);
            double d = dx * dx + dz * dz;
            if (d < best) {
                best = d;
                nearest = player;
            }
        }
        if (nearest == null) {
            return pos;
        }
        int top = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ()) + 1;
        int[] range = MobBand.range(nearest.getBlockY(), world.getBottomY(), top);
        if (range == null) {
            // Below bottomY + 1: vanilla's own "no attempt" check right after this call returns.
            return new BlockPos(pos.getX(), world.getBottomY(), pos.getZ());
        }
        return new BlockPos(pos.getX(), MathHelper.nextBetween(world.random, range[0], range[1]), pos.getZ());
    }
}
