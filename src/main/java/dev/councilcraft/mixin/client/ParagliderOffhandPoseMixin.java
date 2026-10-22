package dev.councilcraft.mixin.client;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.api.item.Paraglider;

/**
 * PPM70. Paragliders 20.1.4 ({@code tictim.paraglider.mixin.MixinPlayerModel}) raises both arms and
 * straightens the legs when the main-hand paraglider has the Paragliding flag. The same pose, at the
 * same point (after the biped angles, before the sleeves copy them), for a flagged offhand paraglider.
 * The server sets the flag only on the stack in use ({@code ParagliderOffhandMovementMixin}).
 */
@Mixin(PlayerEntityModel.class)
public abstract class ParagliderOffhandPoseMixin {
    private static final float COUNCILCRAFT$ARM_ROTATION = 3.3831854f;

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V"),
        require = 1)
    private void councilcraft$offhandGliderPose(LivingEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch, CallbackInfo ci) {
        ItemStack offHand = entity.getOffHandStack();
        if (offHand.getItem() instanceof Paraglider p && p.isParagliding(offHand)) {
            BipedEntityModel<?> model = (BipedEntityModel<?>) (Object) this;
            model.leftArm.pitch = COUNCILCRAFT$ARM_ROTATION;
            model.leftArm.yaw = 0.0f;
            model.rightArm.pitch = COUNCILCRAFT$ARM_ROTATION;
            model.rightArm.yaw = 0.0f;
            model.leftLeg.pitch = 0.0f;
            model.rightLeg.pitch = 0.0f;
        }
    }
}
