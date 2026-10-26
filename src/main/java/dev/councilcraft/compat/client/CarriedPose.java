package dev.councilcraft.compat.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/** PT23. A carried entity's render pose: no rotation, and nothing to interpolate between frames. */
@Environment(EnvType.CLIENT)
public final class CarriedPose {
    private CarriedPose() {
    }

    public static void steady(Entity entity) {
        if (entity == null) {
            return;
        }
        entity.setYaw(0.0f);
        entity.setPitch(0.0f);
        entity.prevYaw = 0.0f;
        entity.prevPitch = 0.0f;
        if (entity instanceof LivingEntity living) {
            living.bodyYaw = 0.0f;
            living.prevBodyYaw = 0.0f;
            living.headYaw = 0.0f;
            living.prevHeadYaw = 0.0f;
        }
        entity.prevX = entity.getX();
        entity.prevY = entity.getY();
        entity.prevZ = entity.getZ();
        entity.lastRenderX = entity.getX();
        entity.lastRenderY = entity.getY();
        entity.lastRenderZ = entity.getZ();
    }
}
