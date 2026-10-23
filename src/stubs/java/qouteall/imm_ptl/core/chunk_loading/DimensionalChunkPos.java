package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

/**
 * Compile-time stand-in for Immersive Portals 5.2.0's class of the same name (imm_ptl_core jar nested inside
 * Immersive Portals, which the compiler cannot see). PPM100. Not packed into our jar.
 */
public class DimensionalChunkPos {
    public final RegistryKey<World> dimension = null;

    public ChunkPos getChunkPos() {
        throw new UnsupportedOperationException("stub");
    }
}
