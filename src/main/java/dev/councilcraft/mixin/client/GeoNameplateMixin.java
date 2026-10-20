package dev.councilcraft.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.nameplate.util.NameplateRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * PM14e (S83), compat only. Nameplate 1.1.4 enables its GeckoLib hooks only for the mod id "geckolib3";
 * the pack runs GeckoLib 4 (id "geckolib", used by Naturalist's renderers), and those hooks name methods
 * GeckoLib 4.8.4 no longer has. This is the same pair of hooks Nameplate applies to vanilla mob renderers:
 * draw the plate at the head of render, and hide the vanilla name label for mobs (the plate carries it).
 */
@Mixin(GeoEntityRenderer.class)
public abstract class GeoNameplateMixin<T extends Entity> extends EntityRenderer<T> {
    private GeoNameplateMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @Inject(
        method = "render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void councilcraft$nameplate(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof MobEntity mob) {
            NameplateRender.renderNameplate(this, mob, matrices, vertexConsumers, this.dispatcher, this.getTextRenderer(), !mob.isInvisible(), light);
        }
    }

    @Inject(method = "hasLabel(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void councilcraft$noVanillaLabel(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof MobEntity) {
            cir.setReturnValue(false);
        }
    }
}
