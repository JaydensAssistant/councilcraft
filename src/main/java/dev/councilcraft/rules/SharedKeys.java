package dev.councilcraft.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * PPM66. Which key bindings a physical key press reaches. Pure, testable without a client.
 *
 * <p>Vanilla 1.20.1 keeps one binding per key ({@code KeyBinding.KEY_TO_BINDINGS}), so only the
 * last-registered binding on a shared key ever fires. CouncilCraft lets every binding on that key
 * fire. The unknown (unbound) key reaches nothing: vanilla would fire one arbitrary unbound binding
 * for a GLFW "unknown key" event, and firing all of them would be worse.
 */
public final class SharedKeys {
    private SharedKeys() {
    }

    /** Every binding in {@code all} whose key equals {@code key}, in iteration order; none for {@code unknown}. */
    public static <B, K> List<B> bindingsFor(Iterable<B> all, Function<B, K> keyOf, K key, K unknown) {
        List<B> matches = new ArrayList<>();
        if (key == null || key.equals(unknown)) {
            return matches;
        }
        for (B binding : all) {
            if (Objects.equals(keyOf.apply(binding), key)) {
                matches.add(binding);
            }
        }
        return matches;
    }
}
