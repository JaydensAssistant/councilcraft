package dev.councilcraft.compat;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeFogRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiMutableFogRenderParam;
import dev.councilcraft.compat.client.PortalFade;

/**
 * PT45 (amends PT31), shaders off. During the portal fade, a per-frame override of Distant Horizons' far fog (never
 * the DH config API, which saves to disk). The fade starts from vanilla's terrain fog at the crossing (start and end
 * blocks, divided by the LOD distance L to get DH's far-fog fractions) and eases to the user's own far-fog values:
 * far fog start = lerp(s0, user start, e), end = lerp(e0, user end, e), max thickness 1; min thickness and density
 * stay the user's. There is no destination hold. Client only, loaded only when Distant Horizons and Immersive
 * Portals are both present.
 */
public final class PortalFadeDhFog extends DhApiBeforeFogRenderEvent {
    private PortalFadeDhFog() {
    }

    public static void register() {
        DhApi.events.bind(DhApiBeforeFogRenderEvent.class, new PortalFadeDhFog());
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
        float[] from = PortalFade.fogFractions(PortalFade.startFogStart(), PortalFade.startFogEnd(), chunks * 16);
        if (from == null) {
            return;
        }
        DhApiMutableFogRenderParam fog = event.value.getFogRenderParam();
        fog.setFarFogStartPercent(lerp(from[0], fog.getFarFogStartPercent(), e));
        fog.setFarFogEndPercent(lerp(from[1], fog.getFarFogEndPercent(), e));
        fog.setFarFogMaxThickness(1.0F);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
