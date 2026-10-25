package dev.councilcraft.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * PPM78. The "Vanilla look" box on the Graphics Preset screen: it turns off the animation and texture extras
 * (Fresh Animations and its compats, incl. Vanilla Backport's freshly_animated and freshly_animated_legacy (PT4), Punchy and its packs, Eating Animation's mod-support pack, Drigo's lanterns,
 * Connected Paths, better grass, connected glass and Bibliophilia with our book priority rules). Vanilla Tweaks,
 * Better Leaves, the atlas packs and printed copies stay on. Unticking puts the packs back in the shipped order.
 */
public final class VanillaLook {
    private VanillaLook() {}

    /** The shipped resource pack order (pack/config/yosbr/options.txt, lowest priority first). */
    public static final List<String> DEFAULT_ORDER = List.of(
        "fabric", "continuity:default", "file/Bibliophilia 1.17 (1.20.1).zip", "file/councilcraft-printed-copies.zip",
        "file/councilcraft-book-priority.zip", "file/vanillatweaks-councilcraft.zip", "file/Better-Leaves-9.6.zip",
        "file/Antique_Atlas_4_Compat_v1.4.zip", "file/councilcraft-atlas-ru.zip", "punchy:punchy",
        "file/Modded Swords x Punchy! 1.20.1 - 1.21.1 v1.5.zip", "file/Drigo-3D-Lanterns-x-Punchy.zip",
        "minecraft:supporteatinganimation", "file/FreshAnimations_v1.11.1_1.20+.zip", "vanillabackport:freshly_animated_legacy",
        "vanillabackport:freshly_animated",
        "file/5.0 FA Illager Mod Compats.zip", "file/-1.21.2 Fresh Moves v3.1 (With Animated Eyes).zip",
        "file/Connected-Paths.zip", "file/Connected-Bricks.zip", "file/Connected-Rocks.zip",
        "file/councilcraft-connected-glass.zip", "file/councilcraft-bettergrass-ru.zip", "lambdabettergrass:default");

    /** Packs the Vanilla look turns off. */
    public static final Set<String> OFF = Set.of(
        "punchy:punchy", "file/Modded Swords x Punchy! 1.20.1 - 1.21.1 v1.5.zip", "file/Drigo-3D-Lanterns-x-Punchy.zip",
        "minecraft:supporteatinganimation", "file/FreshAnimations_v1.11.1_1.20+.zip", "vanillabackport:freshly_animated_legacy",
        "vanillabackport:freshly_animated",
        "file/5.0 FA Illager Mod Compats.zip", "file/-1.21.2 Fresh Moves v3.1 (With Animated Eyes).zip",
        "file/Connected-Paths.zip", "file/Connected-Bricks.zip", "file/Connected-Rocks.zip",
        "file/councilcraft-connected-glass.zip", "file/councilcraft-bettergrass-ru.zip", "lambdabettergrass:default",
        "file/Bibliophilia 1.17 (1.20.1).zip", "file/councilcraft-book-priority.zip");

    /**
     * Whether the Vanilla look is on: at least one of its packs exists and is switchable, and none of those is enabled.
     *
     * @param switchable packs that exist and can be turned off (not always-enabled)
     */
    public static boolean isOn(Collection<String> enabled, Predicate<String> switchable) {
        boolean any = false;
        for (String pack : OFF) {
            if (!switchable.test(pack)) continue;
            any = true;
            if (enabled.contains(pack)) return false;
        }
        return any;
    }

    /**
     * The enabled pack list (lowest priority first) with the Vanilla look on or off. On removes every {@link #OFF}
     * pack. Off adds back each available {@link #OFF} pack right after the nearest pack below it in
     * {@link #DEFAULT_ORDER} that is enabled (else right before the nearest enabled pack above it, else on top).
     */
    public static List<String> apply(List<String> enabled, boolean on, Predicate<String> available) {
        List<String> out = new ArrayList<>(enabled);
        if (on) {
            out.removeIf(OFF::contains);
            return out;
        }
        for (int i = 0; i < DEFAULT_ORDER.size(); i++) {
            String pack = DEFAULT_ORDER.get(i);
            if (!OFF.contains(pack) || out.contains(pack) || !available.test(pack)) continue;
            int at = -1;
            for (int j = i - 1; j >= 0 && at < 0; j--) {
                int k = out.indexOf(DEFAULT_ORDER.get(j));
                if (k >= 0) at = k + 1;
            }
            for (int j = i + 1; j < DEFAULT_ORDER.size() && at < 0; j++) {
                int k = out.indexOf(DEFAULT_ORDER.get(j));
                if (k >= 0) at = k;
            }
            out.add(at < 0 ? out.size() : at, pack);
        }
        return out;
    }
}
