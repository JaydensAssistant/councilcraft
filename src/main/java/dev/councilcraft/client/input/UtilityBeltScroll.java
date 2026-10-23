package dev.councilcraft.client.input;

import de.rubixdev.inventorio.config.PlayerSettings;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

/**
 * PPM67. Hold the utility-scroll modifier (default Left Alt) and the mouse wheel cycles Inventorio's
 * utility belt; without it the wheel stays on the hotbar. Inventorio's own "scroll wheel moves the
 * utility belt" player setting is left as the player sets it (default off).
 *
 * <p>Loaded only when Inventorio is present. The wheel hook is {@code UtilityScrollMixin}.
 */
@Environment(EnvType.CLIENT)
public final class UtilityBeltScroll {
    public static final String KEY = "key.councilcraft.utility_scroll";
    public static final String CATEGORY = "key.categories.councilcraft";

    private static KeyBinding modifier;

    private UtilityBeltScroll() {
    }

    public static void register() {
        modifier = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(KEY, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));
    }

    /** The modifier key as the player bound it (PPM88 hints name it), or null before registration. */
    public static KeyBinding modifier() {
        return modifier;
    }

    /**
     * Called for a hotbar scroll. True when the modifier is held and the scroll went to the utility
     * belt instead; the hotbar then does not move. Direction matches Inventorio's "regular" mode.
     */
    public static boolean scroll(PlayerEntity player, double amount) {
        if (modifier == null || !modifier.isPressed() || amount == 0.0) {
            return false;
        }
        PlayerInventoryAddon addon = PlayerInventoryAddon.Companion.getInventoryAddon(player);
        if (addon == null) {
            return false;
        }
        addon.switchToNextUtility(-(int) Math.signum(amount), PlayerSettings.skipEmptyUtilitySlots.getBoolValue());
        return true;
    }
}
