package dev.councilcraft.rules;

import java.util.Map;

/**
 * PPM67. Controller radial menu defaults. Controlify's stock eight are toggle HUD, perspective, drop
 * stack, chat, swap hands, pick block, screenshot and player list. Pick block leaves the D-pad (now
 * the utility belt, pack/kubejs/assets/controlify/controllers/default_bind/default.json) and stays in
 * the radial; three slots become the pack's extra keys a controller otherwise cannot reach. Players can
 * still change every slot. Only a new controller config takes these defaults.
 */
public final class ControllerRadialDefaults {
    /** Radial slot, then the key binding (translation key) it gets. */
    public static final Map<Integer, String> EXTRAS = Map.of(
        0, "key.niftycarts.action",
        6, "key.smallships.ship_sail",
        7, "supplementaries.keybind.quiver"
    );
    /** Controlify registers every other mod's key binding under this namespace. */
    public static final String MODDED_NAMESPACE = "fabric-key-binding-api-v1";

    private ControllerRadialDefaults() {
    }

    /** Controlify's identifier path for a modded key binding (ControlifyBindings.registerModdedBindings). */
    public static String bindingPath(String translationKey) {
        return translationKey.toLowerCase().replaceAll("[^a-z0-9/._-]", "_").trim();
    }

    /** Translation key of the extra for this slot, or null to keep Controlify's default. */
    public static String extraFor(int slot) {
        return EXTRAS.get(slot);
    }
}
