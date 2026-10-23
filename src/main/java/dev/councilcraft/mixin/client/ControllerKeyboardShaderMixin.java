package dev.councilcraft.mixin.client;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * PPM87. Controlify 2.0.4+1.20.1's {@code controlify:gui_textured} render type (on-screen keyboard
 * keys) draws textured quads with the position-color GUI shader, so the keys show as red/green
 * gradients. The shader becomes the position-texture-color one, which matches its vertex format.
 *
 * <p>Applied only with Controlify, and only with that exact build (see the plugin's pinned list),
 * because the target is a compiler-named lambda. Extends {@link RenderLayer} as the target does, for
 * the protected {@code RenderPhase.ShaderProgram}.
 */
@Pseudo
@Mixin(targets = "dev.isxander.controlify.utils.render.ExtraRenderTypes", remap = false)
public abstract class ControllerKeyboardShaderMixin extends RenderLayer {
    private ControllerKeyboardShaderMixin() {
        super("councilcraft_unused", VertexFormats.POSITION, VertexFormat.DrawMode.QUADS, 0, false, false, () -> {}, () -> {});
    }

    @ModifyArg(method = "lambda$static$0",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayer$MultiPhaseParameters$Builder;program(Lnet/minecraft/client/render/RenderPhase$ShaderProgram;)Lnet/minecraft/client/render/RenderLayer$MultiPhaseParameters$Builder;",
            remap = true),
        require = 1)
    private static ShaderProgram councilcraft$positionTexColorShader(ShaderProgram guiProgram) {
        return new ShaderProgram(GameRenderer::getPositionTexColorProgram);
    }
}
