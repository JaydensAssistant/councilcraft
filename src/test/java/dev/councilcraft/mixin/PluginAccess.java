package dev.councilcraft.mixin;

import java.util.Map;
import java.util.Set;

/** Test door to the package-private pin and optional-mod checks of {@link CouncilCraftMixinPlugin}. */
public final class PluginAccess {
    private PluginAccess() {
    }

    public static boolean matches(String mixin, Map<String, String> versions) {
        return CouncilCraftMixinPlugin.pinnedVersionMatches(mixin, versions::get);
    }

    public static boolean applies(String mixin, Set<String> mods) {
        return CouncilCraftMixinPlugin.shouldApply(mixin, mods::contains);
    }
}
