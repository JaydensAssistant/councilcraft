package dev.councilcraft.mixin;

import dev.councilcraft.rules.MobBand;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM81 (2). A despawnable Overworld monster outside {@link MobBand#BAND} blocks above/below every player despawns
 * at 1 in {@link MobBand#FAR_DESPAWN_CHANCE} per tick, so mobs in unseen caves stop holding the monster cap. The
 * same persistence and {@code canImmediatelyDespawn} rules as vanilla's despawn apply (name tags, picked-up items,
 * mounts, persistent mobs never despawn).
 */
@Mixin(MobEntity.class)
public abstract class MobBandDespawnMixin extends LivingEntity {
    protected MobBandDespawnMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract boolean isPersistent();

    @Shadow
    public abstract boolean cannotDespawn();

    @Shadow
    public abstract boolean canImmediatelyDespawn(double distanceSquared);

    @Inject(method = "checkDespawn", at = @At("TAIL"))
    private void councilcraft$farBandDespawn(CallbackInfo ci) {
        World world = this.getWorld();
        if (this.isRemoved() || world.isClient() || world.getRegistryKey() != World.OVERWORLD
            || this.getType().getSpawnGroup() != SpawnGroup.MONSTER || this.isPersistent() || this.cannotDespawn()) {
            return;
        }
        PlayerEntity closest = null;
        for (PlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            if (MobBand.inBand(this.getY(), player.getY())) {
                return;
            }
            if (closest == null || player.squaredDistanceTo(this) < closest.squaredDistanceTo(this)) {
                closest = player;
            }
        }
        if (closest != null && this.random.nextInt(MobBand.FAR_DESPAWN_CHANCE) == 0
            && this.canImmediatelyDespawn(closest.squaredDistanceTo(this))) {
            this.discard();
        }
    }
}
