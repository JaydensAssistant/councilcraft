package dev.councilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.councilcraft.rules.CartHint;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PPM67. Mounting a cart-pulling animal shows "Press Left Shift to Dismount · Z to attach a cart".
 * The key is Nifty Carts' live action binding, localized; nothing is added when it is unbound or
 * Nifty Carts is absent. Narration reads the same line.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MountCartHintMixin {
    @ModifyExpressionValue(
        method = "onEntityPassengersSet",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/text/MutableText;"
        ),
        require = 1
    )
    private MutableText councilcraft$cartHint(MutableText dismount, @Local(ordinal = 0) Entity vehicle) {
        if (!CartHint.pullsCarts(Registries.ENTITY_TYPE.getId(vehicle.getType()).toString())) {
            return dismount;
        }
        KeyBinding action = KeyBindingAccess.councilcraft$keysById().get(CartHint.NIFTY_CARTS_ACTION);
        if (action == null || action.isUnbound()) {
            return dismount;
        }
        return dismount.append(Text.literal(" \u00b7 ")).append(Text.translatable(CartHint.HINT, action.getBoundKeyLocalizedText()));
    }
}
