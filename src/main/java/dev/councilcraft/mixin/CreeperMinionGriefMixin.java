package dev.councilcraft.mixin;

import fuzs.mutantmonsters.world.entity.CreeperMinion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S224. The minion tracker can toggle {@code DestroysBlocks}. That flag is what
 * the minion passes into {@code MutatedExplosion.create}. Forcing it false keeps
 * the toggle honest; {@link MutatedExplosionGriefMixin} is the guarantee even if
 * this method is bypassed.
 */
@Mixin(CreeperMinion.class)
public abstract class CreeperMinionGriefMixin {
    // Mod method, not a Minecraft method, so the name is not in the Yarn mappings.
    @Inject(method = "canDestroyBlocks", at = @At("HEAD"), cancellable = true, remap = false)
    private void councilcraft$neverDestroyBlocks(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
