package dev.councilcraft.mixin;

import dev.councilcraft.rules.CoinValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * S123. Numismatics 1.1.0 sets the worth of each coin in the enum's static
 * setup: SPUR 1, BEVEL 8, SPROCKET 16, COG 64, CROWN 512, SUN 4096. Those
 * five numbers that are not already 1 are replaced with the pack values.
 *
 * <p>SPUR is left as the stock {@code iconst_1}. That same instruction is also
 * BEVEL's enum ordinal, so rewriting every 1 would renumber the enum.
 * SPUR's pack value is 1, so leaving it is the correct result.
 *
 * <p>{@code @Pseudo} is here for the same reason as the toolbox mixin: loading
 * {@code Coin} needs Create classes nested inside another jar, and the compiler
 * cannot see them. {@code CouncilCraft} still throws at startup if the class
 * is missing. A stock number that is no longer in the static setup fails here,
 * because each constant is required.
 */
@Pseudo
@Mixin(targets = "dev.ithundxr.createnumismatics.content.backend.Coin", remap = false)
public abstract class CoinValueMixin {
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 8), remap = false)
    private static int councilcraft$bevel(int stock) {
        return CoinValues.replaceStock(stock);
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 16), remap = false)
    private static int councilcraft$sprocket(int stock) {
        return CoinValues.replaceStock(stock);
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 64), remap = false)
    private static int councilcraft$cog(int stock) {
        return CoinValues.replaceStock(stock);
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 512), remap = false)
    private static int councilcraft$crown(int stock) {
        return CoinValues.replaceStock(stock);
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 4096), remap = false)
    private static int councilcraft$sun(int stock) {
        return CoinValues.replaceStock(stock);
    }
}
