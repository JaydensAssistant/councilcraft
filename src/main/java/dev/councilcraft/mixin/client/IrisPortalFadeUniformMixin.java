package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.client.PortalFade;
import java.util.function.IntSupplier;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT13 / PT45, shaders on. Iris 1.7.6 registers the per-frame dhRenderDistance uniform as the last call in
 * {@code CommonUniforms.generalCommonUniforms}; shader packs put their Distant Horizons border fog there. The supplier
 * returns {@link PortalFade#effectiveDhRenderDistance}: V (the real-chunk edge) while Immersive Portals renders a
 * portal view, so the destination's own fog fades the portal-view terrain, and after a crossing a distance that
 * recedes from V to the real DH distance over 3 s (cubic ease-in, no hold). DHCompat.getRenderDistance itself
 * (shadows, matrices) is unchanged. Also adds the uniform cc_portalFade (0..1) for shader packs that want it.
 * Applied only with Iris 1.7.6, Distant Horizons and Immersive Portals; pinned in CouncilCraftMixinPlugin. If the
 * target is missing the mixin is skipped: no fade, no crash.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public abstract class IrisPortalFadeUniformMixin {
    @Unique
    private static final Logger councilcraft$LOGGER = LoggerFactory.getLogger("councilcraft");
    @Unique
    private static boolean councilcraft$logged;

    @ModifyArg(method = "generalCommonUniforms", remap = false,
        slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=dhRenderDistance")),
        at = @At(value = "INVOKE", ordinal = 0, remap = false,
            target = "Lnet/irisshaders/iris/gl/uniform/UniformHolder;uniform1i(Lnet/irisshaders/iris/gl/uniform/UniformUpdateFrequency;Ljava/lang/String;Ljava/util/function/IntSupplier;)Lnet/irisshaders/iris/gl/uniform/UniformHolder;"),
        index = 2)
    private static IntSupplier councilcraft$fadeDhRenderDistance(IntSupplier real) {
        return () -> PortalFade.effectiveDhRenderDistance(real.getAsInt());
    }

    @Inject(method = "generalCommonUniforms", at = @At("TAIL"), remap = false)
    private static void councilcraft$portalFadeUniform(UniformHolder uniforms, @Coerce Object updateNotifier,
            @Coerce Object directives, CallbackInfo ci) {
        uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "cc_portalFade", PortalFade::fade);
        if (!councilcraft$logged) {
            councilcraft$logged = true;
            councilcraft$LOGGER.info("CouncilCraft: portal fade (PT13) hooked Iris dhRenderDistance and added cc_portalFade.");
        }
    }
}
