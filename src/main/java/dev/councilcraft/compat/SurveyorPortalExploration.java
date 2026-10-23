package dev.councilcraft.compat;

import folk.sisby.surveyor.SurveyorExploration;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;

/**
 * PPM100, loaded only when Immersive Portals (imm_ptl_core) and Surveyor are both present.
 * Immersive Portals sends chunks itself and cancels vanilla chunk tracking, so Surveyor's only exploration hook
 * (its mixin on vanilla chunk sending) rarely fires and Antique Atlas shows blank areas. This feeds every chunk
 * Immersive Portals starts sending to a player into that player's Surveyor exploration, the same call Surveyor's
 * own hook makes. No mixin. The signal is static, so it is connected once per game, at the first server start.
 */
public final class SurveyorPortalExploration {
    private static boolean connected;

    private SurveyorPortalExploration() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (connected) {
                return;
            }
            connected = true;
            NewChunkTrackingGraph.beginWatchChunkSignal.connect((player, pos) ->
                SurveyorExploration.of(player).addChunk(pos.dimension, pos.getChunkPos(), false));
        });
    }
}
