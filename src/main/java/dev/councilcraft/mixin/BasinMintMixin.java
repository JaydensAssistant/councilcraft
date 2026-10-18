package dev.councilcraft.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import dev.councilcraft.rules.MintPriority;
import java.util.Optional;
import net.minecraft.recipe.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sprocket and cog minting lose to the vanilla diamond-block craft when the
 * diamonds arrive first. Create sorts matches by ingredient count, so the mint
 * recipe wins when both match at once. The press locks the diamond-block recipe
 * as soon as nine diamonds are present, and extra items (the mold, the tenth
 * diamond) do not make {@code matchBasinRecipe} reject it. {@code applyBasinRecipe}
 * consumes that locked recipe before it asks whether it still matches.
 *
 * <p>The target is {@code @Pseudo} for the same reason as {@link ToolboxInsertMixin}:
 * Create is not a compile dependency. {@code getInputInventory} returns
 * {@code SmartInventory}; the compile-only stub uses that exact return type.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity", remap = false)
public abstract class BasinMintMixin {
    @Shadow
    @Mutable
    protected Recipe<?> currentRecipe;

    @Shadow
    protected abstract Optional<BasinBlockEntity> getBasin();

    @Inject(
        method = "matchBasinRecipe(Lnet/minecraft/class_1860;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void councilcraft$rejectDiamondBlockWhileMinting(Recipe<?> recipe, CallbackInfoReturnable<Boolean> cir) {
        if (MintPriority.basinSuppressesDiamondBlock(this.getBasin().orElse(null), recipe)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * A cycle that already chose diamond blocks must not consume. Clearing the
     * recipe makes Create's own null check return before {@code BasinRecipe.apply}.
     * The press then schedules a fresh lookup, and the mint recipe is selected.
     * That in-progress cycle produces nothing.
     */
    @Inject(method = "applyBasinRecipe()V", at = @At("HEAD"), remap = false)
    private void councilcraft$abortDiamondBlockAlreadyChosen(CallbackInfo ci) {
        if (MintPriority.basinSuppressesDiamondBlock(this.getBasin().orElse(null), this.currentRecipe)) {
            this.currentRecipe = null;
        }
    }
}
