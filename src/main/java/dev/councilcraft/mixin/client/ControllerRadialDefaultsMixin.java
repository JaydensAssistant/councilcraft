package dev.councilcraft.mixin.client;

import dev.councilcraft.rules.ControllerRadialDefaults;
import dev.isxander.controlify.api.bind.ControlifyBindApi;
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
 * {@link ControllerRadialDefaults#EXTRAS} the default becomes that binding instead (a mod key, or
 * PPM105's Next quiver arrow), when Controlify has it registered (its registry is locked before any
 * controller config is made). Existing controller configs keep their saved slots. Applied only with Controlify.
 */
@Pseudo
@Mixin(targets = "dev.isxander.controlify.controller.input.InputComponent$Config", remap = false)
public abstract class ControllerRadialDefaultsMixin {
    @Shadow
    public Identifier[] radialActions;

    @Inject(method = "setDefaultRadialAction", at = @At("TAIL"), require = 1)
    private void councilcraft$packExtras(int index, CallbackInfo ci) {
        String extra = ControllerRadialDefaults.extraFor(index);
        if (extra == null) {
            return;
        }
        Identifier id = new Identifier(extra);
        if (ControlifyBindApi.get().getAllBindIds().anyMatch(id::equals)) {
            this.radialActions[index] = id;
        }
    }
}
