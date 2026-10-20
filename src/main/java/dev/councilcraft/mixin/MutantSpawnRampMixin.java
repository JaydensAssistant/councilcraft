package dev.councilcraft.mixin;

import dev.councilcraft.rules.MutantSpawnRamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PM45. Natural MONSTER spawns: the Mutant Monsters entries of the local spawn list are picked so that
 * mutants make up {@link MutantSpawnRamp#share} of hostile spawns at this position, and never inside
 * 2,500 blocks of 0,0. The other entries keep their vanilla weights among themselves. Lists without a
 * mutant entry are untouched. /summon, spawners and Chemical X snow golems do not pass through here.
 * The picked entry comes from the same list vanilla checks again in {@code canSpawn}.
 */
@Mixin(SpawnHelper.class)
public abstract class MutantSpawnRampMixin {
    @Shadow
    private static Pool<SpawnSettings.SpawnEntry> getSpawnEntries(
        ServerWorld world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
        SpawnGroup spawnGroup, BlockPos pos, @Nullable RegistryEntry<Biome> biomeEntry
    ) {
        throw new AssertionError();
    }

    @Inject(method = "pickRandomSpawnEntry", at = @At("HEAD"), cancellable = true)
    private static void councilcraft$mutantRamp(
        ServerWorld world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
        SpawnGroup spawnGroup, Random random, BlockPos pos,
        CallbackInfoReturnable<Optional<SpawnSettings.SpawnEntry>> cir
    ) {
        if (spawnGroup != SpawnGroup.MONSTER) {
            return;
        }
        Pool<SpawnSettings.SpawnEntry> pool = getSpawnEntries(world, structureAccessor, chunkGenerator, spawnGroup, pos, null);
        List<SpawnSettings.SpawnEntry> mutants = new ArrayList<>();
        List<SpawnSettings.SpawnEntry> others = new ArrayList<>();
        for (SpawnSettings.SpawnEntry entry : pool.getEntries()) {
            (MutantSpawnRamp.isMutant(Registries.ENTITY_TYPE.getId(entry.type).toString()) ? mutants : others).add(entry);
        }
        if (mutants.isEmpty()) {
            return;
        }
        double share = MutantSpawnRamp.share(pos.getX() + 0.5, pos.getZ() + 0.5);
        double chance = others.isEmpty() ? (share > 0.0 ? 1.0 : 0.0)
            : MutantSpawnRamp.pickChance(share, meanPack(others), meanPack(mutants));
        List<SpawnSettings.SpawnEntry> from = random.nextDouble() < chance ? mutants : others;
        cir.setReturnValue(from.isEmpty() ? Optional.empty() : Pool.of(from).getOrEmpty(random));
    }

    private static double meanPack(List<SpawnSettings.SpawnEntry> entries) {
        double weighted = 0.0;
        double total = 0.0;
        for (SpawnSettings.SpawnEntry entry : entries) {
            int weight = entry.getWeight().getValue();
            weighted += weight * (entry.minGroupSize + entry.maxGroupSize) / 2.0;
            total += weight;
        }
        return total <= 0.0 ? 1.0 : weighted / total;
    }
}
