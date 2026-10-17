package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;

/** S228. Depth Strider does nothing while Dolphin's Grace is active. Either effect alone is unchanged. */
public final class DepthStriderRule {
    private DepthStriderRule() {
    }

    public static int effectiveLevel(int depthStriderLevel, boolean dolphinsGraceActive) {
        if (dolphinsGraceActive) {
            return CouncilCraftRules.DEPTH_STRIDER_UNDER_DOLPHINS_GRACE;
        }
        return depthStriderLevel;
    }
}
