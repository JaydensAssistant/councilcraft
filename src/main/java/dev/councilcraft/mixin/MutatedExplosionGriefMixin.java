package dev.councilcraft.mixin;

import dev.councilcraft.rules.CreeperGrief;
import fuzs.mutantmonsters.world.entity.CreeperMinion;
import fuzs.mutantmonsters.world.entity.mutant.MutantCreeper;
import fuzs.mutantmonsters.world.level.MutatedExplosion;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S224. Mutant Monsters 8.0.8 builds creeper and minion blasts in
 * {@code MutatedExplosion.create}, which picks a destruction type from the mobGriefing
 * gamerule. The gamerule stays on (piglin bartering, sheep, allays and vanilla
 * creepers still break blocks). These two entities are forced to {@code KEEP},
 * which still runs entity damage inside {@code explode()} and then breaks nothing.
 *
 * <p>Creeper-shard and creeper-minion-egg blasts are different entities and are
 * left alone. The minion's "destroys blocks" toggle cannot override this.
 */
@Mixin(MutatedExplosion.class)
public abstract class MutatedExplosionGriefMixin {
    // Mod method. The parameter types are Minecraft types and are remapped with the jar.
    @Inject(method = "getExplosionInteraction", at = @At("HEAD"), cancellable = true, remap = false)
    private static void councilcraft$noBlockDamage(
        World world,
        Entity entity,
        World.ExplosionSourceType sourceType,
        CallbackInfoReturnable<Explosion.DestructionType> cir
    ) {
        boolean mutantCreeperOrMinion = entity instanceof MutantCreeper || entity instanceof CreeperMinion;
        if (CreeperGrief.suppressBlockDamage(mutantCreeperOrMinion)) {
            cir.setReturnValue(Explosion.DestructionType.KEEP);
        }
    }
}
