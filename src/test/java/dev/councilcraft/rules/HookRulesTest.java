package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Rules that do not need a world. The mixins call these same methods. */
class HookRulesTest {
    @Test
    void riptideThreeIsHalfTheVanillaLaunch() {
        float vanilla = RiptideLaunch.launchBlocksPerTick(3, 3.0f);
        float ours = RiptideLaunch.launchBlocksPerTick(3, RiptideLaunch.halvedCoefficient(3.0f));
        assertEquals(3.0f, vanilla, 0.0001f);
        assertEquals(1.5f, ours, 0.0001f);
        assertEquals(vanilla * 0.5f, ours, 0.0001f);
    }

    @Test
    void everyRiptideLevelIsHalved() {
        for (int level = 1; level <= 3; level++) {
            float vanilla = RiptideLaunch.launchBlocksPerTick(level, 3.0f);
            float ours = RiptideLaunch.launchBlocksPerTick(level, RiptideLaunch.halvedCoefficient(3.0f));
            assertEquals(vanilla * 0.5f, ours, 0.0001f);
        }
    }

    @Test
    void depthStriderIsIgnoredOnlyWhileDolphinsGraceIsActive() {
        assertEquals(0, DepthStriderRule.effectiveLevel(3, true));
        assertEquals(3, DepthStriderRule.effectiveLevel(3, false));
        assertEquals(0, DepthStriderRule.effectiveLevel(0, true));
        assertEquals(1, DepthStriderRule.effectiveLevel(1, false));
    }

    @Test
    void paragliderAcceptsPhantomMembraneAndStillAcceptsLeather() {
        assertTrue(ParagliderRepair.accepts(false, true));
        assertTrue(ParagliderRepair.accepts(true, false));
        assertFalse(ParagliderRepair.accepts(false, false));
    }

    @Test
    void goldOnlyPoolIsDroppedAndMixedPoolKeepsTheOtherShoes() {
        assertTrue(GoldHorseshoeLoot.withoutGold(List.of("horseshoes:gold_horseshoes")).isEmpty());
        assertEquals(
            List.of("horseshoes:iron_horseshoes", "horseshoes:diamond_horseshoes"),
            GoldHorseshoeLoot.withoutGold(List.of(
                "horseshoes:iron_horseshoes",
                "horseshoes:diamond_horseshoes",
                "horseshoes:gold_horseshoes"
            ))
        );
        assertEquals(
            List.of("horseshoes:iron_horseshoes"),
            GoldHorseshoeLoot.withoutGold(List.of("horseshoes:iron_horseshoes"))
        );
    }

    @Test
    void mutantCreeperAndMinionSuppressBlockDamage() {
        assertTrue(CreeperGrief.suppressBlockDamage(true));
        assertFalse(CreeperGrief.suppressBlockDamage(false));
    }

    @Test
    void handshakeRequiresTheSameVersion() {
        assertTrue(HandshakeRule.versionsMatch("0.1.0", "0.1.0"));
        assertFalse(HandshakeRule.versionsMatch("0.1.0", "0.2.0"));
        assertFalse(HandshakeRule.versionsMatch("0.1.0", null));
        assertFalse(HandshakeRule.versionsMatch(null, "0.1.0"));
        assertEquals(
            "This server requires the CouncilCraft mod. Install the CouncilCraft modpack.",
            HandshakeRule.MISSING_MOD
        );
        assertEquals(
            "CouncilCraft version mismatch. Server has 0.1.0, client has 0.2.0.",
            HandshakeRule.versionMismatch("0.1.0", "0.2.0")
        );
    }

    @Test
    void rejectionIsHeldUntilThePlayerWouldBeAccepted() {
        LoginRejection rejection = new LoginRejection();
        assertNull(rejection.take());
        rejection.record(HandshakeRule.MISSING_MOD);
        rejection.record(HandshakeRule.versionMismatch("0.1.0", "0.2.0"));
        assertEquals(HandshakeRule.MISSING_MOD, rejection.take());
        assertNull(rejection.take());
    }

    @Test
    void sacksCannotNest() {
        assertFalse(SackNesting.canFitInsideContainerItems());
    }
}
