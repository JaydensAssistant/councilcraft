package dev.councilcraft.mixin.client;

import fuzs.mutantmonsters.client.renderer.entity.AlternateMobRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.nameplate.util.NameplateRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PM14e (S83), compat only. Nameplate 1.1.4 draws its plate from the head of MobEntityRenderer.render.
 * Mutant Monsters' AlternateMobRenderer overrides that method without calling super, so mutants got no
 * plate. This makes the same Nameplate call, with the same arguments, at the head of the override.
 * Nameplate's own hasLabel hook (on MobEntityRenderer) already applies to these renderers.
 */
@Mixin(AlternateMobRenderer.class)
public abstract class MutantNameplateMixin<T extends MobEntity, M extends EntityModel<T>> extends MobEntityRenderer<T, M> {
    private MutantNameplateMixin(EntityRendererFactory.Context context, M model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(
        method = "render(Lnet/minecraft/entity/mob/MobEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void councilcraft$nameplate(T mob, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        NameplateRender.renderNameplate(this, mob, matrices, vertexConsumers, this.dispatcher, this.getTextRenderer(), this.isVisible(mob), light);
    }
}
