package folk.sisby.surveyor;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

/**
 * Compile-time stand-in for Surveyor 1.2.4+1.20's interface of the same name. PPM100. Surveyor's jar was built
 * with a newer Loom than ours, so it cannot be a modCompileOnly dependency. Not packed into our jar.
 */
public interface SurveyorExploration {
    static SurveyorExploration of(ServerPlayerEntity player) {
        throw new UnsupportedOperationException("stub");
    }

    default void addChunk(RegistryKey<World> worldKey, ChunkPos pos, boolean shared) {
        throw new UnsupportedOperationException("stub");
    }
}
