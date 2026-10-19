package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.CouncilCraftRules;
import org.junit.jupiter.api.Test;

/** Horse tiers, the speed cap, ridden step height, and the toolbox rule. */
class HorseRulesTest {
    @Test
    void ironAndDiamondSpeedAreHalfTheModDefaults() {
        assertEquals(0.05f, HorseshoeTiers.speed(CouncilCraftRules.IRON_HORSESHOES_ID, 0.1f), 0.0001f);
        assertEquals(0.075f, HorseshoeTiers.speed(CouncilCraftRules.DIAMOND_HORSESHOES_ID, 0.15f), 0.0001f);
        assertEquals(0.1f, HorseshoeTiers.speed(CouncilCraftRules.NETHERITE_HORSESHOES_ID, 0.1f), 0.0001f);
        assertEquals(0.2f, HorseshoeTiers.speed(CouncilCraftRules.GOLD_HORSESHOE_ID, 0.2f), 0.0001f);
    }

    @Test
    void stepHeightFollowsTheTierAndOnlyNetheriteResistsKnockback() {
        assertEquals(2.1f, HorseshoeTiers.stepHeight(CouncilCraftRules.IRON_HORSESHOES_ID), 0.0001f);
        assertEquals(2.6f, HorseshoeTiers.stepHeight(CouncilCraftRules.DIAMOND_HORSESHOES_ID), 0.0001f);
        assertEquals(3.1f, HorseshoeTiers.stepHeight(CouncilCraftRules.NETHERITE_HORSESHOES_ID), 0.0001f);
        assertEquals(0.0f, HorseshoeTiers.stepHeight(CouncilCraftRules.GOLD_HORSESHOE_ID), 0.0001f);
        assertEquals(0.1f, HorseshoeTiers.knockbackResistance(CouncilCraftRules.NETHERITE_HORSESHOES_ID), 0.0001f);
        assertEquals(0.0f, HorseshoeTiers.knockbackResistance(CouncilCraftRules.IRON_HORSESHOES_ID), 0.0001f);
        assertEquals(7.0f, CouncilCraftRules.NETHERITE_HORSESHOE_ARMOR, 0.0001f);
        assertEquals(3.5f, CouncilCraftRules.IRON_HORSESHOE_ARMOR, 0.0001f);
        assertEquals(5.5f, CouncilCraftRules.DIAMOND_HORSESHOE_ARMOR, 0.0001f);
    }

    @Test
    void riddenShoesRaiseStepAndEverythingElseKeepsTheVanillaHeight() {
        assertEquals(2.1f, HorseStep.height(true, 2.1f, 1.0f), 0.0001f);
        assertEquals(1.0f, HorseStep.height(false, 2.1f, 1.0f), 0.0001f);
        assertEquals(1.5f, HorseStep.height(true, 0.0f, 1.5f), 0.0001f);
        assertEquals(1.5f, HorseStep.height(false, 3.1f, 1.5f), 0.0001f);
    }

    @Test
    void speedCapSitsJustAboveANetheriteHorse() {
        assertEquals(0.44f, HorseSpeed.cap(0.50f), 0.0001f);
        assertEquals(0.44f, HorseSpeed.cap(0.44f + 0.1f), 0.0001f);
        assertEquals(0.30f, HorseSpeed.cap(0.30f), 0.0001f);
        assertEquals(0.44f, CouncilCraftRules.HORSE_SPEED_CAP, 0.0001f);
    }

    @Test
    void toolboxAcceptsWhitelistItemsAndEmptyInsertsOnly() {
        assertTrue(ToolboxFilter.allows(true, false));
        assertTrue(ToolboxFilter.allows(false, true));
        assertFalse(ToolboxFilter.allows(false, false));
    }
}
