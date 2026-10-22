package dev.councilcraft.mixin.client;

import java.util.Map;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Every registered key binding by translation key, to read another mod's binding (Nifty Carts' action key). */
@Mixin(KeyBinding.class)
public interface KeyBindingAccess {
    @Accessor("KEYS_BY_ID")
    static Map<String, KeyBinding> councilcraft$keysById() {
        throw new AssertionError();
    }
}
