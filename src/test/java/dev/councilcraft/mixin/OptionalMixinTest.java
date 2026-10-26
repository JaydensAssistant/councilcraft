package dev.councilcraft.mixin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
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
    void release047HooksNeedTheirMods() {
        for (String m : new String[] {"InventorioSwordSlotMixin", "InventorioAttackSwapMixin", "InventorioToolBeltSaveMixin", "client.UtilityScrollMixin"}) {
            assertTrue(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin." + m, Set.of("inventorio")::contains), m);
            assertFalse(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin." + m, Set.<String>of()::contains), m);
        }
        for (String m : new String[] {"ConnectedPathsConfigMixin", "ConnectedPathsPropertiesMixin", "ConnectedPathsStateMixin"}) {
            assertTrue(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin.client." + m, Set.of("connectedpaths")::contains), m);
            assertFalse(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin.client." + m, Set.<String>of()::contains), m);
        }
        assertFalse(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin.client.ControllerRadialDefaultsMixin", Set.<String>of()::contains));
        // Vanilla-only hooks are never gated.
        assertTrue(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin.client.SharedKeyMixin", Set.<String>of()::contains));
        assertTrue(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin.client.MountCartHintMixin", Set.<String>of()::contains));
    }

    @Test
    void otherMixinsAreNotGated() {
        assertTrue(CouncilCraftMixinPlugin.shouldApply("dev.councilcraft.mixin.SackNestingMixin", Set.<String>of()::contains));
    }

    // ---- 0.4.10 ----

    @Test
    void release0410HooksNeedTheirMods() {
        String swap = "dev.councilcraft.mixin.InventorioSwapHandsMixin";
        assertTrue(CouncilCraftMixinPlugin.shouldApply(swap, Set.of("inventorio")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(swap, Set.of("controlify")::contains));
        String screen = "dev.councilcraft.mixin.client.ControllerInventorioScreenMixin";
        assertTrue(CouncilCraftMixinPlugin.shouldApply(screen, Set.of("controlify", "inventorio")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(screen, Set.of("controlify")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(screen, Set.of("inventorio")::contains));
        String keyboard = "dev.councilcraft.mixin.client.ControllerKeyboardShaderMixin";
        assertTrue(CouncilCraftMixinPlugin.shouldApply(keyboard, Set.of("controlify")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(keyboard, Set.<String>of()::contains));
    }

    @Test
    void keyboardShaderFixIsPinnedToControlify204() {
        String keyboard = "dev.councilcraft.mixin.client.ControllerKeyboardShaderMixin";
        assertTrue(CouncilCraftMixinPlugin.pinnedVersionMatches(keyboard, Map.of("controlify", "2.0.4+1.20.1-fabric")::get));
        assertFalse(CouncilCraftMixinPlugin.pinnedVersionMatches(keyboard, Map.of("controlify", "2.0.5+1.20.1-fabric")::get));
        // Unpinned mixins ignore versions.
        assertTrue(CouncilCraftMixinPlugin.pinnedVersionMatches("dev.councilcraft.mixin.client.ControllerInventorioScreenMixin",
            Map.of("controlify", "9.9.9")::get));
    }

    // ---- 0.4.14 ----

    @Test
    void netherPortalRatioNeedsImmersivePortals520() {
        String ratio = "dev.councilcraft.mixin.NetherPortalRatioMixin";
        assertTrue(CouncilCraftMixinPlugin.shouldApply(ratio, Set.of("immersive_portals")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(ratio, Set.of("imm_ptl_core")::contains));
        assertTrue(CouncilCraftMixinPlugin.pinnedVersionMatches(ratio, Map.of("immersive_portals", "5.2.0")::get));
        assertFalse(CouncilCraftMixinPlugin.pinnedVersionMatches(ratio, Map.of("immersive_portals", "5.2.1")::get));
        // A wrong version is an ERROR naming what breaks, not a quiet warning.
        String[] pin = CouncilCraftMixinPlugin.pinFor(ratio);
        assertTrue(pin.length == 4 && pin[3].contains("CC-5"));
        assertTrue(CouncilCraftMixinPlugin.pinFor("dev.councilcraft.mixin.client.ControllerKeyboardShaderMixin").length == 3);
    }

    // ---- 0.4.15 ----

    @Test
    void portalFadeHooksNeedTheirModsAndVersions() {
        String iris = "dev.councilcraft.mixin.client.IrisPortalFadeUniformMixin";
        String ip = "dev.councilcraft.mixin.client.IpDimensionChangeFadeMixin";
        assertTrue(CouncilCraftMixinPlugin.shouldApply(iris, Set.of("iris", "distanthorizons", "imm_ptl_core")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(iris, Set.of("iris", "imm_ptl_core")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(iris, Set.of("distanthorizons", "imm_ptl_core")::contains));
        assertTrue(CouncilCraftMixinPlugin.shouldApply(ip, Set.of("distanthorizons", "imm_ptl_core")::contains));
        assertFalse(CouncilCraftMixinPlugin.shouldApply(ip, Set.of("imm_ptl_core")::contains));
        assertTrue(CouncilCraftMixinPlugin.pinnedVersionMatches(iris, Map.of("iris", "1.7.6+mc1.20.1")::get));
        assertFalse(CouncilCraftMixinPlugin.pinnedVersionMatches(iris, Map.of("iris", "1.7.7+mc1.20.1")::get));
        assertTrue(CouncilCraftMixinPlugin.pinnedVersionMatches(ip, Map.of("immersive_portals", "5.2.0")::get));
        assertFalse(CouncilCraftMixinPlugin.pinnedVersionMatches(ip, Map.of("immersive_portals", "5.2.1")::get));
        // The shader hook names what breaks; the IP trigger only warns (the tick fallback still starts the fade).
        String[] pin = CouncilCraftMixinPlugin.pinFor(iris);
        assertTrue(pin.length == 4 && pin[3].contains("PT13"));
        assertTrue(CouncilCraftMixinPlugin.pinFor(ip).length == 3);
    }
}
