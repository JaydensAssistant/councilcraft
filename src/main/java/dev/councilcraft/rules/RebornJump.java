package dev.councilcraft.rules;

/**
 * PM24. Reborn shrinks the player with Pehkui's BASE scale. BASE feeds Pehkui's MOTION scale, and Pehkui
 * multiplies the player's whole movement vector (vertical too) by MOTION, so a 60% player jumps about 0.75
 * blocks and steps 0.36 blocks. Pehkui's JUMP_HEIGHT scale multiplies the jump velocity instead. Jump apex
 * grows faster than linearly with launch speed, so 1/BASE would overshoot (about 2 blocks at 60%); the
 * factor here is the one that puts the scaled apex back on the vanilla apex. Step height is a plain
 * distance, so its factor is exactly 1/BASE (vanilla 0.6, still clears slabs).
 */
public final class RebornJump {
    /** Vanilla player jump velocity, gravity and vertical drag (LivingEntity.travel). */
    static final double JUMP_VELOCITY = 0.42;
    static final double GRAVITY = 0.08;
    static final double DRAG = 0.98;

    private RebornJump() {
    }

    /** Highest point of a jump launched at {@code velocity} when every move is multiplied by {@code motion}. */
    public static double apex(double velocity, double motion) {
        double y = 0.0;
        double best = 0.0;
        double v = velocity;
        for (int tick = 0; tick < 200 && v > 0.0; tick++) {
            y += v * motion;
            best = Math.max(best, y);
            v = (v - GRAVITY) * DRAG;
        }
        return best;
    }

    /** JUMP_HEIGHT scale that gives a vanilla jump apex at this BASE scale. 1 for full size. */
    public static float jumpScale(float base) {
        if (base >= 0.9999f || base <= 0f) {
            return 1f;
        }
        double target = apex(JUMP_VELOCITY, 1.0);
        double lo = 1.0;
        double hi = 1.0 / base;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2.0;
            if (apex(JUMP_VELOCITY * mid, base) < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (float) ((lo + hi) / 2.0);
    }

    /** STEP_HEIGHT scale that keeps the vanilla step height at this BASE scale. */
    public static float stepScale(float base) {
        if (base >= 0.9999f || base <= 0f) {
            return 1f;
        }
        return 1f / base;
    }
}
