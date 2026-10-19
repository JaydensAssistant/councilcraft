package dev.councilcraft.mixin;

import dev.councilcraft.rules.PatrolEvoker;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S121 / S242. After a patrol captain is actually in the world, one roll in
 * four spawns an evoker beside that captain.
 *
 * <p>Illager Invasion's mixin replaces the stored patrol member with a marauder
 * half the time, including the captain. This mixin does not touch that local.
 * It adds an evoker. It does not replace the pillager, the vindicator or the
 * marauder.
 *
 * <p>The evoker is a real patrol member when the captain has a patrol target:
 * spawn reason PATROL sets {@code Patrolling}, and the target is copied.
 * If the captain has no target, the evoker still appears beside the patrol
 * but {@code Patrolling} is cleared. That is the fallback.
 */
@Mixin(net.minecraft.world.spawner.PatrolSpawner.class)
public abstract class PatrolEvokerMixin {
    @Inject(
        method = "spawnPillager",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V",
            shift = At.Shift.AFTER
        )
    )
    private void councilcraft$evoker(
        ServerWorld world,
        BlockPos pos,
        Random random,
        boolean leader,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!leader || !PatrolEvoker.joins(world.getRandom().nextInt(PatrolEvoker.sides()))) {
            return;
        }
        PatrolEntity captain = nearestLeader(world, pos);
        EvokerEntity evoker = EntityType.EVOKER.create(world);
        if (evoker == null) {
            return;
        }
        BlockPos beside = pos.add(1, 0, 0);
        evoker.refreshPositionAndAngles(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5, 0.0f, 0.0f);
        evoker.initialize(world, world.getLocalDifficulty(pos), SpawnReason.PATROL, null, null);
        boolean joined = captain != null && captain.hasPatrolTarget();
        if (joined) {
            evoker.setPatrolTarget(captain.getPatrolTarget());
        } else {
            ((PatrolEntityAccess) evoker).councilcraft$setPatrolling(false);
        }
        world.spawnEntityAndPassengers(evoker);
    }

    private static PatrolEntity nearestLeader(ServerWorld world, BlockPos pos) {
        Box area = new Box(pos).expand(1.0);
        for (PatrolEntity entity : world.getEntitiesByClass(PatrolEntity.class, area, PatrolEntity::isPatrolLeader)) {
            return entity;
        }
        return null;
    }
}
