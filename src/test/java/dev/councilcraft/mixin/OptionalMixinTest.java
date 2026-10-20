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
    void otherMixinsAreNotGated() {
        assertTrue(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin.SackNestingMixin", Set.<String>of()::contains));
    }
}
