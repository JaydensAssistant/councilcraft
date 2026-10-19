package dev.councilcraft.rules;

/**
 * S223 / S244. Step height changes only while someone is riding.
 * An unridden horse, and a ridden horse with no tier shoes, keeps whatever
 * step height that entity spawned with (1.0 for a horse, 1.5 for a camel).
 */
public final class HorseStep {
    private HorseStep() {
    }

    public static float height(boolean ridden, float shoeStep, float vanillaStep) {
        if (ridden && shoeStep > 0.0f) {
            return shoeStep;
        }
        return vanillaStep;
    }
}
