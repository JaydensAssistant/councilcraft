package dev.councilcraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * CC-5. The Nether is 1:1 with the Overworld. Immersive Portals 5.2.0 lights nether portals itself
 * ({@code netherPortalMode} "normal") through {@code IntrinsicPortalGeneration.intrinsicToNether}, built in the
 * class initializer as {@code new CustomPortalGeneration([overworld], the_nether, 8, 1, ...)}. That hardcoded 8:1
 * ignores the dimension type's {@code coordinate_scale} (our nether-1to1-amplified datapack), so portals linked 1:8.
 * This sets that constructor's {@code spaceRatioFrom} (argument 2) to 1; {@code intrinsicFromNether} is
 * {@code getReverse()} of it, so the way back is 1:1 too. IP's see-through portals are unchanged.
 *
 * <p>Pinned to Immersive Portals 5.2.0 (CouncilCraftMixinPlugin logs an ERROR and skips it on any other version).
 * {@code require = 1}: if the constructor call is gone the game stops at startup instead of silently linking 1:8.
 * Only the first {@code CustomPortalGeneration} built there is the intrinsic one; the second is the "adaptive"
 * mode's diligent generation, which this pack does not use.
 */
@Pseudo
@Mixin(targets = "qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration", remap = false)
public abstract class NetherPortalRatioMixin {
    @ModifyArg(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lqouteall/imm_ptl/core/portal/custom_portal_gen/CustomPortalGeneration;<init>(Ljava/util/List;Lnet/minecraft/class_5321;IIZLqouteall/imm_ptl/core/portal/custom_portal_gen/form/PortalGenForm;Lqouteall/imm_ptl/core/portal/custom_portal_gen/PortalGenTrigger;Ljava/util/List;Ljava/util/List;)V",
            ordinal = 0,
            remap = false
        ),
        index = 2,
        require = 1,
        remap = false
    )
    private static int councilcraft$oneToOneNether(int overworldBlocksPerNetherBlock) {
        return 1;
    }
}
