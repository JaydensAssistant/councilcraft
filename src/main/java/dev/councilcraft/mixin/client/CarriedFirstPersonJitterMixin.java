package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.client.CarriedPose;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import tschipp.carryon.client.render.CarriedObjectRender;

/** PT23 (first person). Same as {@link CarriedHeadJitterMixin}, on the entity handed to the render dispatcher. */
@Mixin(value = CarriedObjectRender.class, remap = false)
public abstract class CarriedFirstPersonJitterMixin {
    @ModifyArg(method = "drawFirstPersonEntity", index = 0, at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private static Entity councilcraft$steadyPose(Entity entity) {
        CarriedPose.steady(entity);
        return entity;
    }
}
