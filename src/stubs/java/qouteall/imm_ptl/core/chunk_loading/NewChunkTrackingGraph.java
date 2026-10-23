package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.server.network.ServerPlayerEntity;
import qouteall.q_misc_util.my_util.SignalBiArged;

/**
 * Compile-time stand-in for Immersive Portals 5.2.0's class of the same name (imm_ptl_core jar nested inside
 * Immersive Portals, which the compiler cannot see). PPM100. Not packed into our jar.
 */
public final class NewChunkTrackingGraph {
    public static final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> beginWatchChunkSignal = null;

    private NewChunkTrackingGraph() {
    }
}
