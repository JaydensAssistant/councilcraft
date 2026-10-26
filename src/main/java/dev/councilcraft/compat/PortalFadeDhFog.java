package dev.councilcraft.compat;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeFogRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiMutableFogRenderParam;
import dev.councilcraft.compat.client.PortalDhDepth;
import dev.councilcraft.compat.client.PortalFade;

/**
 * PT13, shaders off. During the portal fade, Distant Horizons' far fog moves part way (by the fade, at most
 * the dip) toward full fog (the values DH itself uses for blindness/lava: start 0, end 1, thickness 1,
 * density 1) and back to the player's settings as the fade ends. Client only, loaded only when Distant Horizons and Immersive Portals are both present.
 */
public final class PortalFadeDhFog extends DhApiBeforeFogRenderEvent {
    private PortalFadeDhFog() {
    }

    public static void register() {
        DhApi.events.bind(DhApiBeforeFogRenderEvent.class, new PortalFadeDhFog());
    }

    @Override
    public void beforeRender(DhApiCancelableEventParam<EventParam> event) {
        float fade = PortalFade.fade();
        if (fade <= 0.0F || PortalDhDepth.inPortalView()) {
            return;
        }
        DhApiMutableFogRenderParam fog = event.value.getFogRenderParam();
        fog.setFarFogStartPercent(lerp(fog.getFarFogStartPercent(), 0.0F, fade));
        fog.setFarFogEndPercent(lerp(fog.getFarFogEndPercent(), 1.0F, fade));
        fog.setFarFogMinThickness(lerp(fog.getFarFogMinThickness(), 1.0F, fade));
        fog.setFarFogMaxThickness(lerp(fog.getFarFogMaxThickness(), 1.0F, fade));
        fog.setFarFogDensity(lerp(fog.getFarFogDensity(), 1.0F, fade));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
