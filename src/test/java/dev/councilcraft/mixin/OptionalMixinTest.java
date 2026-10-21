package dev.councilcraft.mixin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class OptionalMixinTest {
    private static final String SACKS = "dev.councilcraft.mixin.SackEncumbranceMixin";

    @Test
    void inventorioSackWeightNeedsBothMods() {
        assertTrue(CouncilCraftMixinPlugin.shouldApply(SACKS, Set.of("supplementaries", "inventorio")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(SACKS, Set.of("supplementaries")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(SACKS, Set.of("inventorio")::contains));
    }

    @Test
    void carryOnWeightNeedsCarryOnAndSupplementaries() {
        String carry = "dev.councilcraft.mixin.CarryOnWeightMixin";
        assertTrue(CouncilCraftMixinPlugin.shouldApply(carry, Set.of("carryon", "supplementaries")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(carry, Set.of("carryon")::contains));
    }

    @Test
    void portalShaderFixesNeedIrisAndImmersivePortals() {
        String hand = "dev.councilcraft.mixin.client.IrisPortalHandMixin";
        String depth = "dev.councilcraft.mixin.client.IrisDhPortalDepthMixin";
        assertTrue(CouncilCraftMixinPlugin.shouldApply(hand, Set.of("iris", "imm_ptl_core")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(hand, Set.of("iris")::contains));
        assertTrue(CouncilCraftMixinPlugin.shouldApply(depth, Set.of("iris", "distanthorizons", "imm_ptl_core")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(depth, Set.of("iris", "imm_ptl_core")::contains));
    }

    @Test
    void otherMixinsAreNotGated() {
        assertTrue(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin.SackNestingMixin", Set.<String>of()::contains));
    }
}
