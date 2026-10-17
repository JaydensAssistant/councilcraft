package dev.councilcraft.mixin;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.RiptideLaunch;
import net.minecraft.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * S226. {@code TridentItem.onStoppedUsing} (Mojmap {@code releaseUsing}) launches with
 * {@code 3.0 * (1 + riptideLevel) / 4}. Halving the 3.0 literal halves every level.
 * Riptide III goes from 3.0 blocks/tick of added velocity to 1.5 before friction.
 *
 * <p>The method runs on both sides. A client without this mixin would predict the
 * full launch and then rubber-band, which is why the mod is required on the client.
 */
@Mixin(TridentItem.class)
public abstract class TridentRiptideMixin {
    @ModifyConstant(method = "onStoppedUsing", constant = @Constant(floatValue = CouncilCraftRules.RIPTIDE_VANILLA_COEFFICIENT))
    private float councilcraft$halfRiptide(float vanillaCoefficient) {
        return RiptideLaunch.halvedCoefficient(vanillaCoefficient);
    }
}
