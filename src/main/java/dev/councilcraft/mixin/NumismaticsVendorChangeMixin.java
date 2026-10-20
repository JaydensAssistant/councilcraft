package dev.councilcraft.mixin;

import dev.councilcraft.coin.VendorChange;
import dev.ithundxr.createnumismatics.content.coins.CoinItem;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * PM34. Numismatics' price behaviour (vendors, brass depositors) takes the exact listed coins when the
 * player pays by hand. If that fails, pay by total value with change instead. Card and bank payments take
 * the other branch and are unchanged; the vendor still receives the listed coins (addCoinsToSelf).
 */
@Pseudo
@Mixin(targets = "dev.ithundxr.createnumismatics.content.backend.behaviours.SliderStylePriceBehaviour", remap = false)
public abstract class NumismaticsVendorChangeMixin {
    @Redirect(
        method = "deduct(Lnet/minecraft/class_1657;Lnet/minecraft/class_1268;ZLdev/ithundxr/createnumismatics/content/backend/ReasonHolder;)Z",
        remap = false,
        at = @At(value = "INVOKE", remap = false,
            target = "Ldev/ithundxr/createnumismatics/content/coins/CoinItem;extract(Lnet/minecraft/class_1657;Lnet/minecraft/class_1268;Ljava/util/Map;Z)Z")
    )
    private boolean councilcraft$payWithChange(PlayerEntity player, Hand hand, Map<?, Integer> prices, boolean onlyHand) {
        // The original call first: the exact listed coins, as Numismatics does it.
        if (CoinItem.extract(player, hand, prices, onlyHand)) {
            return true;
        }
        return !onlyHand && VendorChange.payByValue(player, VendorChange.priceValue(prices));
    }
}
