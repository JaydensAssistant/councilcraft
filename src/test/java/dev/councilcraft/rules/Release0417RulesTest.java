package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 0.4.17: PT15-PT24 wiring that unit tests can check without a world. */
class Release0417RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void playerCarryingIsOnAndGuarded() throws IOException {
        assertTrue(read("pack/config/carryon-common.json").contains("\"pickupPlayers\": true,"));
        assertTrue(read("src/main/resources/councilcraft.mixins.json").contains("\"CarryEscapeMixin\""));
        assertTrue(read("src/main/java/dev/councilcraft/CouncilCraft.java").contains("dev.councilcraft.carry.BabyCarry.register();"));
        String carry = read("src/main/java/dev/councilcraft/carry/BabyCarry.java");
        assertTrue(carry.contains("if (Reborn.running(carrier) || !Reborn.running(baby))"));
        assertTrue(carry.contains("UseEntityCallback.EVENT.addPhaseOrdering(BEFORE_CARRY_ON, Event.DEFAULT_PHASE);"));
        assertTrue(read("src/main/resources/assets/councilcraft/lang/en_us.json")
            .contains("\"councilcraft.carry.cooldown\": \"Can't pick them up for %s\""));
    }

    @Test
    void dciGuardsArePinnedTo307() throws IOException {
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        assertTrue(plugin.contains("{\"dev.councilcraft.mixin.DciLeaveGuardMixin\", \"dcintegration-fabric\", \"3.0.7\","));
        assertTrue(plugin.contains("{\"dev.councilcraft.mixin.DciCommandGuardMixin\", \"dcintegration-fabric\", \"3.0.7\","));
    }

    @Test
    void feedingRefusalsHaveMessages() throws IOException {
        String lang = read("src/main/resources/assets/councilcraft/lang/en_us.json");
        assertTrue(lang.contains("\"councilcraft.feed.refused.too_far\": \"Too far to feed %s. Get within 3 blocks.\""));
        assertTrue(lang.contains("\"councilcraft.feed.refused.full\": \"%s is full.\""));
    }

    @Test
    void printerNamesBothPhotoInks() throws IOException {
        String lang = read("src/main/resources/assets/councilcraft/lang/en_us.json");
        assertTrue(lang.contains("\"item.councilcraft.process_ink_bucket\": \"Process Ink Bucket\""));
        assertTrue(lang.contains("Black Dye: black-and-white photos"));
        assertTrue(lang.contains("Process Ink: color photos and albums"));
        assertFalse(lang.contains("warning"));
    }

    @Test
    void pvpHeadsAvoidRhinoInvisibleGetUuid() throws IOException {
        // PT24: Entity#getUUID is not visible to Rhino on this KubeJS ("Cannot find function getUUID").
        String js = read("pack/kubejs/server_scripts/councilcraft_heads.js");
        assertFalse(js.contains("getUUID()"));
        assertTrue(js.contains("if (direct.equals(attacker)) return attacker"));
        assertTrue(js.contains("rec.putString('uuid', String(attacker.getGameProfile().getId()))"));
    }

    @Test
    void carryEscapeCountsOnlyARealSneakDismount() throws IOException {
        String mixin = read("src/main/java/dev/councilcraft/mixin/CarryEscapeMixin.java");
        assertTrue(mixin.contains("method = \"tickRiding\""));
        assertTrue(mixin.contains("if (carried && !self.hasVehicle())"));
    }

    @Test
    void versionIs0417() throws IOException {
        assertTrue(read("gradle.properties").contains("mod_version=0.4.17\n"));
        assertTrue(read("pack/pack.toml").contains("version = \"0.4.17\""));
    }
}
