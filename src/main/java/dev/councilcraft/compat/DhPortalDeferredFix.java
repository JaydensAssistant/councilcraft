package dev.councilcraft.compat;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeDeferredRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

/**
 * PM21, client only, loaded only when Distant Horizons and Immersive Portals are both present.
 * DH 3.3.3's own Immersive Portals compat cancels DhApiBeforeRenderEvent (the opaque LOD pass) while a
 * portal view renders. Under Iris the deferred pass (transparent LODs and DH fog) fires its own
 * DhApiBeforeDeferredRenderEvent, which nothing cancelled, so it drew into portal views with the wrong
 * level and camera. This cancels that event under the same condition DH uses for the opaque pass.
 */
public final class DhPortalDeferredFix extends DhApiBeforeDeferredRenderEvent {
    private DhPortalDeferredFix() {
    }

    public static void register() {
        DhApi.events.bind(DhApiBeforeDeferredRenderEvent.class, new DhPortalDeferredFix());
    }

    @Override
    public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
        if (PortalRendering.isRendering()) {
            event.cancelEvent();
        }
    }
}
