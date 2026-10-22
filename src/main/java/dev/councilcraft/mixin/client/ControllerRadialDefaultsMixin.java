package dev.councilcraft.mixin.client;

import dev.councilcraft.rules.ControllerRadialDefaults;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM67. Controlify 2.0.4 fills an empty or invalid radial-menu slot from
 * {@code InputComponent.Config.setDefaultRadialAction}. For the slots in
 * {@link ControllerRadialDefaults#EXTRAS} the default becomes that mod key instead, when the key
 * exists. Existing controller configs keep their saved slots. Applied only with Controlify.
 */
@Pseudo
@Mixin(targets = "dev.isxander.controlify.controller.input.InputComponent$Config", remap = false)
public abstract class ControllerRadialDefaultsMixin {
    @Shadow
    public Identifier[] radialActions;

    @Inject(method = "setDefaultRadialAction", at = @At("TAIL"), require = 1)
    private void councilcraft$packExtras(int index, CallbackInfo ci) {
        String key = ControllerRadialDefaults.extraFor(index);
        if (key != null && KeyBindingAccess.councilcraft$keysById().containsKey(key)) {
            this.radialActions[index] = new Identifier(ControllerRadialDefaults.MODDED_NAMESPACE, ControllerRadialDefaults.bindingPath(key));
        }
    }
}
