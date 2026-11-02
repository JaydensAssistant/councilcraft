package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.cart.CartDetachRules;
import dev.councilcraft.ledger.GraveRobDecision;
import dev.councilcraft.respawn.RebornStatusEffect;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** CC-125 / CC-139 / CC-142A / CC-155 / CC-159 / CC-173A unit coverage for 0.4.27. */
public class Release0427RulesTest {
    private static final double EPS = 1e-9;

    @Test
    void handshakeAcceptsPriorAndCurrentClients() {
        assertTrue(HandshakeRule.COMPATIBLE_CLIENT_VERSIONS.contains("0.4.26"));
        assertTrue(HandshakeRule.COMPATIBLE_CLIENT_VERSIONS.contains("0.4.27"));
        assertTrue(HandshakeRule.versionsMatch("0.4.27", "0.4.26"));
        assertTrue(HandshakeRule.versionsMatch("0.4.27", "0.4.27"));
        assertFalse(HandshakeRule.versionsMatch("0.4.27", "0.4.25"));
        assertFalse(HandshakeRule.versionsMatch("0.4.27", "0.5.0"));
        assertFalse(HandshakeRule.versionsMatch("0.4.27", null));
    }

    @Test
    void mutantRampContinuousUncapped() {
        assertEquals(0.0, MutantSpawnRamp.share(2500, 0), EPS);
        assertEquals(0.03, MutantSpawnRamp.share(5000, 0), EPS);
        assertEquals(0.075, MutantSpawnRamp.share(8750, 0), EPS);
        assertTrue(MutantSpawnRamp.share(12374, 0) > 0.075);
        assertEquals(
            (12374 - 2500) * CouncilCraftRules.MUTANT_PER_BLOCK_SHARE,
            MutantSpawnRamp.share(12374, 0),
            EPS
        );
    }

    @Test
    void graveRobLogsNonOwnerOnly() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID looter = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertTrue(GraveRobDecision.shouldLog(looter, owner));
        assertFalse(GraveRobDecision.shouldLog(owner, owner));
        assertFalse(GraveRobDecision.shouldLog(null, owner));
        assertFalse(GraveRobDecision.shouldLog(looter, null));
    }

    @Test
    void reachScaleIsInverseOfBase() {
        assertEquals(1f, RebornStatusEffect.reachScale(1f), 1e-6f);
        assertEquals(1f / 0.6f, RebornStatusEffect.reachScale(0.6f), 1e-6f);
        assertEquals(1f, RebornStatusEffect.reachScale(0f), 1e-6f);
        assertEquals(1f, RebornStatusEffect.reachScale(-1f), 1e-6f);
    }

    @Test
    void cartNeverAutoUnhitchesAndHoldsPuller() {
        assertFalse(CartDetachRules.allowCollisionDetach());
        assertFalse(CartDetachRules.allowDistanceDetach(10.0, 2.0));
        assertFalse(CartDetachRules.shouldHoldPullerBack(2.0, 2.0));
        assertTrue(CartDetachRules.shouldHoldPullerBack(3.0, 2.0));
        assertEquals(1f, CartDetachRules.pullerSlowFactor(2.0, 2.0), 1e-6f);
        assertTrue(CartDetachRules.pullerSlowFactor(5.0, 2.0) < 1f);
        assertTrue(CartDetachRules.pullerSlowFactor(100.0, 2.0) >= 0.2f);
    }

    @Test
    void rebornActiveIsTwoHours() {
        assertEquals(2 * 60, CouncilCraftRules.REBORN_ACTIVE_MINUTES);
    }

    @Test
    void riderChanceStepsFromSpawn() {
        // CC-173A: floor(dist/250)*(0.075/28); 0 within 249; 7.5% at 7,000; ~13.1% at 12,374; no cap.
        assertEquals(0.0, RiderChance.chance(249, 0), EPS);
        assertEquals(0.0, RiderChance.chance(0, 0), EPS);
        assertEquals(CouncilCraftRules.RIDER_PER_STEP, RiderChance.chance(250, 0), EPS);
        assertEquals(0.075, RiderChance.chance(7000, 0), EPS);
        assertEquals(Math.floor(12374 / 250.0) * CouncilCraftRules.RIDER_PER_STEP, RiderChance.chance(12374, 0), EPS);
        assertTrue(RiderChance.chance(12374, 0) > 0.13);
        assertTrue(RiderChance.chance(20000, 0) > RiderChance.chance(12374, 0));
        assertEquals(
            Math.floor(2500 / 250.0) * CouncilCraftRules.RIDER_PER_STEP,
            RiderChance.chance(2500, 0),
            EPS
        );
        assertEquals(
            Math.floor(5000 / 250.0) * CouncilCraftRules.RIDER_PER_STEP,
            RiderChance.chance(5000, 0),
            EPS
        );
    }

}
