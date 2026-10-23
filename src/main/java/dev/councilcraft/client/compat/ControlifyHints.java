package dev.councilcraft.client.compat;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.font.BindingFontHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * PPM88. Controlify reads for the utility-belt hints. Separate class so Controlify types load only
 * when Controlify is present.
 */
public final class ControlifyHints {
    /** Controlify's emulated bindings for Inventorio's prev / next utility keys (D-pad Left / Right in our default.json). */
    private static final Identifier PREV_UTILITY = new Identifier("fabric-key-binding-api-v1", "inventorio.keys.prev_utility");
    private static final Identifier NEXT_UTILITY = new Identifier("fabric-key-binding-api-v1", "inventorio.keys.next_utility");

    private ControlifyHints() {
    }

    public static boolean onController() {
        return ControlifyApi.get().currentInputMode().isController();
    }

    /** The glyph of the button currently bound to prev utility (live, so a rebind shows). */
    public static Text prevUtilityGlyph() {
        return BindingFontHelper.binding(PREV_UTILITY);
    }

    public static Text nextUtilityGlyph() {
        return BindingFontHelper.binding(NEXT_UTILITY);
    }
}
