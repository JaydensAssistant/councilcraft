package dev.councilcraft.compat;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiLevelType;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeFogRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelUnloadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiMutableFogRenderParam;
import dev.councilcraft.compat.client.PortalFade;
import dev.councilcraft.compat.client.PortalFadeLevels;

/**
 * PT31, shaders off. During the portal fade, a per-frame override of Distant Horizons' far fog (never the DH config
 * API, which saves to disk): far fog start = lerp(V/L, user start, e), end = lerp(V/L + 0.02, user end, e), max
 * thickness 1; min thickness and density stay the user's. DH's far fog start/end are fractions of the LOD distance L.
 * Also feeds DH's client level load/unload events to the destination hold. Client only, loaded only when Distant
 * Horizons and Immersive Portals are both present.
 */
public final class PortalFadeDhFog extends DhApiBeforeFogRenderEvent {
    private PortalFadeDhFog() {
    }

    public static void register() {
        DhApi.events.bind(DhApiBeforeFogRenderEvent.class, new PortalFadeDhFog());
        DhApi.events.bind(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent() {
            @Override
            public void onLevelLoad(DhApiEventParam<DhApiLevelLoadEvent.EventParam> event) {
                String dimension = clientDimension(event.value.levelWrapper);
                if (dimension != null) {
                    PortalFadeLevels.loaded(dimension);
                    PortalFade.levelLoaded(dimension);
                }
            }
        });
        DhApi.events.bind(DhApiLevelUnloadEvent.class, new DhApiLevelUnloadEvent() {
            @Override
            public void onLevelUnload(DhApiEventParam<DhApiLevelUnloadEvent.EventParam> event) {
                String dimension = clientDimension(event.value.levelWrapper);
                if (dimension != null) {
                    PortalFadeLevels.unloaded(dimension);
                }
            }
        });
    }

    private static String clientDimension(IDhApiLevelWrapper level) {
        if (level == null || level.getLevelType() != EDhApiLevelType.CLIENT_LEVEL) {
            return null;
        }
        return level.getDimensionName();
    }

    @Override
    public void beforeRender(DhApiCancelableEventParam<EventParam> event) {
        if (!PortalFade.fading()) {
            return;
        }
        float e = PortalFade.progress();
        if (e >= 1.0F) {
            return;
        }
        Integer chunks = DhApi.Delayed.configs == null ? null : DhApi.Delayed.configs.graphics().chunkRenderDistance().getValue();
        if (chunks == null || chunks <= 0) {
            return;
        }
        float edge = fogEdge(PortalFade.vanillaEdge(), chunks * 16);
        if (edge >= 1.0F) {
            return;
        }
        DhApiMutableFogRenderParam fog = event.value.getFogRenderParam();
        fog.setFarFogStartPercent(lerp(edge, fog.getFarFogStartPercent(), e));
        fog.setFarFogEndPercent(lerp(edge + 0.02F, fog.getFarFogEndPercent(), e));
        fog.setFarFogMaxThickness(1.0F);
    }

    /** V / L as a fraction of the LOD distance. */
    static float fogEdge(int vanilla, int lod) {
        return lod <= 0 ? 1.0F : (float) vanilla / lod;
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
