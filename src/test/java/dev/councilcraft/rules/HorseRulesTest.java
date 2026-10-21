package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.CouncilCraftRules;
import org.junit.jupiter.api.Test;

/** Horse tiers, the speed cap, ridden step height, and the toolbox rule. */
class HorseRulesTest {
    @Test
    void ironAndDiamondSpeedAreHalfTheModDefaultsAndGoldSitsBetween() {
        assertEquals(0.05f, HorseshoeTiers.speed(CouncilCraftRules.IRON_HORSESHOES_ID, 0.1f), 0.0001f);
        assertEquals(0.075f, HorseshoeTiers.speed(CouncilCraftRules.DIAMOND_HORSESHOES_ID, 0.15f), 0.0001f);
        assertEquals(0.1f, HorseshoeTiers.speed(CouncilCraftRules.NETHERITE_HORSESHOES_ID, 0.1f), 0.0001f);
        assertEquals(0.0625f, HorseshoeTiers.speed(CouncilCraftRules.GOLD_HORSESHOE_ID, 0.2f), 0.0001f);
        assertEquals(0.2f, HorseshoeTiers.speed("minecraft:stick", 0.2f), 0.0001f);
        assertEquals(4.5f, HorseshoeTiers.armor(CouncilCraftRules.GOLD_HORSESHOE_ID, 1.0f), 0.0001f);
        assertEquals(1.0f, HorseshoeTiers.armor("minecraft:stick", 1.0f), 0.0001f);
    }

    @Test
    void stepHeightFollowsTheTierAndOnlyNetheriteResistsKnockback() {
        assertEquals(2.1f, HorseshoeTiers.stepHeight(CouncilCraftRules.GOLD_HORSESHOE_ID), 0.0001f);
        assertEquals(3.1f, HorseshoeTiers.stepHeight(CouncilCraftRules.IRON_HORSESHOES_ID), 0.0001f);
        assertEquals(4.1f, HorseshoeTiers.stepHeight(CouncilCraftRules.DIAMOND_HORSESHOES_ID), 0.0001f);
        assertEquals(6.1f, HorseshoeTiers.stepHeight(CouncilCraftRules.NETHERITE_HORSESHOES_ID), 0.0001f);
        assertEquals(0.0f, HorseshoeTiers.stepHeight("minecraft:stick"), 0.0001f);
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
    void shoesAddOneToFourBlocksOfJump() {
        assertEquals(1.0f, HorseshoeTiers.jumpBlocks(CouncilCraftRules.GOLD_HORSESHOE_ID), 0.0001f);
        assertEquals(2.0f, HorseshoeTiers.jumpBlocks(CouncilCraftRules.IRON_HORSESHOES_ID), 0.0001f);
        assertEquals(3.0f, HorseshoeTiers.jumpBlocks(CouncilCraftRules.DIAMOND_HORSESHOES_ID), 0.0001f);
        assertEquals(4.0f, HorseshoeTiers.jumpBlocks(CouncilCraftRules.NETHERITE_HORSESHOES_ID), 0.0001f);
        assertEquals(0.0f, HorseshoeTiers.jumpBlocks("minecraft:stick"), 0.0001f);
    }

    @Test
    void tooltipNumbersArePlainAndOneBlockIsSingular() {
        assertEquals("0.0625", HorseshoeTiers.plain(0.0625f));
        assertEquals("4.5", HorseshoeTiers.plain(4.5f));
        assertEquals("2", HorseshoeTiers.plain(2.0f));
        assertEquals("k.one", HorseshoeTiers.blocksKey("k", 1.0f));
        assertEquals("k", HorseshoeTiers.blocksKey("k", 2.0f));
    }

    @Test
    void averageHorseJumpsFiveBlocksAndTheBestEight() {
        assertEquals(5.0, HorseJump.buffedApex(HorseJump.VANILLA_AVERAGE_STRENGTH), 1e-9);
        assertEquals(8.0, HorseJump.buffedApex(HorseJump.VANILLA_BEST_STRENGTH), 1e-9);
        assertEquals(5.0, HorseJump.apex(HorseJump.strength(0.7, 0.0f)), 1e-6);
        assertEquals(12.0, HorseJump.apex(HorseJump.strength(1.0, 4.0f)), 1e-6);
        // Average horse in diamond jumps as high as the best unshod horse.
        assertEquals(HorseJump.strength(1.0, 0.0f), HorseJump.strength(0.7, 3.0f), 1e-6);
    }

    @Test
    void horseFallIsSafeUpToJumpHeightPlusTwo() {
        assertEquals(7.0, HorseFall.safeDistance(HorseJump.strength(0.7, 0.0f)), 1e-6);
        assertEquals(10.0, HorseFall.safeDistance(HorseJump.strength(1.0, 0.0f)), 1e-6);
        assertEquals(14.0, HorseFall.safeDistance(HorseJump.strength(1.0, 4.0f)), 1e-6);
        // Never below vanilla's 6.
        assertEquals(6.0, HorseFall.safeDistance(0.3), 1e-6);
        double average = HorseJump.strength(0.7, 0.0f);
        assertEquals(0, HorseFall.damage(7.0f, 1.0f, average));
        assertEquals(1, HorseFall.damage(9.0f, 1.0f, average));
        assertEquals(3, HorseFall.damage(13.0f, 1.0f, average));
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
