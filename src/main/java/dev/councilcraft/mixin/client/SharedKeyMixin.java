package dev.councilcraft.mixin.client;

import dev.councilcraft.rules.SharedKeys;
import java.util.Map;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM66. One key press reaches every key binding bound to that key.
 *
 * <p>Vanilla's {@code onKeyPressed} and {@code setKeyPressed} look the key up in
 * {@code KEY_TO_BINDINGS}, which holds one binding per key, so a second mod's binding on the same
 * key is dead. These two entry points instead walk every binding ({@code KEYS_BY_ID}) and apply the
 * same press count or held state to each one on that key ({@link SharedKeys}). Each binding's own
 * {@code setPressed} still runs, so sticky (toggle) bindings behave as before. The controls menu
 * still marks shared keys red; that highlight is cosmetic.
 */
@Mixin(KeyBinding.class)
public abstract class SharedKeyMixin {
    @Shadow
    @Final
    private static Map<String, KeyBinding> KEYS_BY_ID;

    @Shadow
    private InputUtil.Key boundKey;

    @Shadow
    private int timesPressed;

    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true, require = 1)
    private static void councilcraft$pressEveryBinding(InputUtil.Key key, CallbackInfo ci) {
        for (KeyBinding binding : SharedKeys.bindingsFor(KEYS_BY_ID.values(), SharedKeyMixin::councilcraft$key, key, InputUtil.UNKNOWN_KEY)) {
            ((SharedKeyMixin) (Object) binding).timesPressed++;
        }
        ci.cancel();
    }

    @Inject(method = "setKeyPressed", at = @At("HEAD"), cancellable = true, require = 1)
    private static void councilcraft$holdEveryBinding(InputUtil.Key key, boolean pressed, CallbackInfo ci) {
        for (KeyBinding binding : SharedKeys.bindingsFor(KEYS_BY_ID.values(), SharedKeyMixin::councilcraft$key, key, InputUtil.UNKNOWN_KEY)) {
            binding.setPressed(pressed);
        }
        ci.cancel();
    }

    @Unique
    private static InputUtil.Key councilcraft$key(KeyBinding binding) {
        return ((SharedKeyMixin) (Object) binding).boundKey;
    }
}
