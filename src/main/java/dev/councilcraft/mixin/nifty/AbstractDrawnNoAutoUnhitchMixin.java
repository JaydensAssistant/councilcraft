package dev.councilcraft.mixin.nifty;

import dev.councilcraft.cart.CartDetachRules;
import net.jmb19905.niftycarts.entity.AbstractDrawnEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * CC-142A. Hitched Nifty carts keep collision and never auto-unhitch on
 * distance or collision. A lagging cart slows the puller. Manual unhitch
 * ({@code setPulling(null)} from player action) is unchanged.
 *
 * <p>{@code @At(remap = true)} is required for the {@code Entity} parameter in
 * {@code setPulling} targets so intermediary {@code class_1297} matches at
 * runtime. Method names stay {@code remap = false} (NiftyCarts own members).
 */
@Pseudo
@Mixin(value = AbstractDrawnEntity.class, remap = false)
public abstract class AbstractDrawnNoAutoUnhitchMixin {
    @Unique
    private static final UUID CC_HOLD_BACK_UUID =
        UUID.nameUUIDFromBytes("councilcraft:cc142a_hold_back".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    @Inject(method = "shouldRemovePulling", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void councilcraft$noCollisionDetach(CallbackInfoReturnable<Boolean> cir) {
        if (!CartDetachRules.allowCollisionDetach()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Suppress the distance check's {@code setPulling(null)} inside {@code pulledTick}.
     * Only redirects when the argument is null (auto-unhitch); non-null setPulling calls pass through.
     */
    @Redirect(
        method = "pulledTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/jmb19905/niftycarts/entity/AbstractDrawnEntity;setPulling(Lnet/minecraft/entity/Entity;)V",
            remap = true
        ),
        remap = false,
        require = 1
    )
    private void councilcraft$noDistanceDetach(AbstractDrawnEntity self, Entity entity) {
        if (entity == null) {
            // Auto-unhitch suppressed. Manual paths use shouldStopPulledTick / other callers.
            return;
        }
        self.setPulling(entity);
    }

    /**
     * Suppress both {@code setPulling(null)} sites in {@code shouldStopPulledTick}
     * (dead/vehicle early-out and collision detach). Still allow null when the
     * puller is gone, dead, or mounted.
     */
    @Redirect(
        method = "shouldStopPulledTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/jmb19905/niftycarts/entity/AbstractDrawnEntity;setPulling(Lnet/minecraft/entity/Entity;)V",
            remap = true
        ),
        remap = false,
        require = 1,
        expect = 2
    )
    private void councilcraft$noStopDetach(AbstractDrawnEntity self, Entity entity) {
        if (entity == null) {
            Entity pulling = self.getPulling();
            // Only allow null when puller is gone / dead / being ridden wrongly — not collision.
            if (pulling == null || !pulling.isAlive() || pulling.hasVehicle()) {
                self.setPulling(null);
            }
            return;
        }
        self.setPulling(entity);
    }

    @Inject(method = "pulledTick", at = @At("TAIL"), remap = false, require = 1)
    private void councilcraft$holdPullerBack(CallbackInfo ci) {
        AbstractDrawnEntity self = (AbstractDrawnEntity) (Object) this;
        Entity pulling = self.getPulling();
        if (!(pulling instanceof LivingEntity living) || self.getWorld().isClient) {
            return;
        }
        Vec3d delta = pulling.getPos().subtract(self.getPos());
        double distance = delta.horizontalLength();
        double spacing = Math.max(1.7 + 0.5 * pulling.getWidth(), 1.0);
        EntityAttributeInstance attr = living.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attr == null) {
            return;
        }
        attr.removeModifier(CC_HOLD_BACK_UUID);
        float factor = CartDetachRules.pullerSlowFactor(distance, spacing);
        if (factor < 0.999f) {
            double amount = factor - 1.0; // negative = slow
            attr.addTemporaryModifier(new EntityAttributeModifier(
                CC_HOLD_BACK_UUID,
                "CouncilCraft cart hold-back",
                amount,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }
}
