package dev.councilcraft.rules;

import java.util.Map;

/**
 * PPM67. Controller radial menu defaults. Controlify's stock eight are toggle HUD, perspective, drop
 * stack, chat, swap hands, pick block, screenshot and player list. Pick block leaves the D-pad (now
 * the utility belt, pack/kubejs/assets/controlify/controllers/default_bind/default.json) and stays in
 * the radial; three slots become the pack's extra keys a controller otherwise cannot reach. Players can
 * still change every slot. Only a new controller config takes these defaults. PPM105: slot 7 was
 * Supplementaries' quiver key, which a radial tap cannot use (it needs holding plus scrolling); it is
 * now our own "Next quiver arrow" action (client/compat/ControlifyQuiverCycle).
 */
public final class ControllerRadialDefaults {
    /** Controlify registers every other mod's key binding under this namespace. */
    public static final String MODDED_NAMESPACE = "fabric-key-binding-api-v1";
    /** PPM105. Our Controlify binding that selects the next arrow in a Supplementaries quiver. */
    public static final String NEXT_QUIVER_ARROW = "councilcraft:next_quiver_arrow";
    /** Radial slot, then the Controlify binding id it gets. */
    public static final Map<Integer, String> EXTRAS = Map.of(
        0, moddedKey("key.niftycarts.action"),
        6, moddedKey("key.smallships.ship_sail"),
        7, NEXT_QUIVER_ARROW
    );

    private ControllerRadialDefaults() {
    }

    /** Controlify's identifier path for a modded key binding (ControlifyBindings.registerModdedBindings). */
    public static String bindingPath(String translationKey) {
        return translationKey.toLowerCase().replaceAll("[^a-z0-9/._-]", "_").trim();
    }

    /** Controlify's binding id for a modded key binding. */
    public static String moddedKey(String translationKey) {
        return MODDED_NAMESPACE + ":" + bindingPath(translationKey);
    }

    /** Controlify binding id of the extra for this slot, or null to keep Controlify's default. */
    public static String extraFor(int slot) {
        return EXTRAS.get(slot);
    }
}
