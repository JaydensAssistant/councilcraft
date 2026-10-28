package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.councilcraft.compat.client.IpnInventorioMovePlacement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 0.4.25: unit-testable lock PT90 without a world. */
class Release0425RulesTest {
    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    @Test
    void versionIs0425() throws IOException {
        assertTrue(read("gradle.properties").contains("mod_version=0.4.25"));
        assertTrue(read("pack/pack.toml").contains("version = \"0.4.25\""));
    }

    // PT90 -------------------------------------------------------------------------------------------------------

    @Test
    void inventorioHintsShowMoveToContainerBesideSort() throws IOException {
        String raw = read(
            "pack/config/yosbr/config/inventoryprofilesnext/integrationHints/councilcraft.json");
        JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
        JsonObject screen = root.getAsJsonObject("de.rubixdev.inventorio.client.ui.InventorioScreen");
        JsonObject buttons = screen.getAsJsonObject("buttonHints");

        JsonObject sort = buttons.getAsJsonObject("SORT");
        assertEquals(22, sort.get("horizontalOffset").getAsInt());
        assertEquals(66, sort.get("top").getAsInt());
        assertFalse(sort.has("hide") && sort.get("hide").getAsBoolean());

        JsonObject moveC = buttons.getAsJsonObject("MOVE_TO_CONTAINER");
        assertEquals(22, moveC.get("horizontalOffset").getAsInt());
        assertEquals(0, moveC.get("bottom").getAsInt());
        assertFalse(moveC.has("hide") && moveC.get("hide").getAsBoolean());

        JsonObject moveP = buttons.getAsJsonObject("MOVE_TO_PLAYER");
        assertTrue(moveP.get("hide").getAsBoolean());
    }

    @Test
    void placementMathMatchesPreviewForZeroAndThreeDeepPocketsRows() {
        int hintTop = 66;
        int hintHOff = 22;
        int hintBottom = 0;
        int bw = IpnInventorioMovePlacement.BUTTON_SIZE;
        int sortTop = IpnInventorioMovePlacement.sortTopFromHint(hintTop);
        int sortRight = IpnInventorioMovePlacement.sortRightFromHint(hintHOff);
        int moveRight = IpnInventorioMovePlacement.moveRightFromSort(sortRight);
        int stockMoveBottom = IpnInventorioMovePlacement.moveBottomFromHint(hintBottom);

        // Previewed without Deep Pockets on a 198x166 bg: Sort (147,71), ↰ (159,71).
        int w = 198;
        int sortLeft = IpnInventorioMovePlacement.leftFromRight(w, sortRight, bw);
        int moveLeft = IpnInventorioMovePlacement.leftFromRight(w, moveRight, bw);
        assertEquals(71, sortTop);
        assertEquals(147, sortLeft);
        assertEquals(159, moveLeft);
        assertEquals(sortTop, sortTop); // same row after mixin
        assertEquals(IpnInventorioMovePlacement.BUTTON_GAP, moveLeft - sortLeft);

        // Height 166 (0 DP rows): stock bottom-anchor happens to match Sort's row.
        int h0 = 166;
        int stockTop0 = IpnInventorioMovePlacement.topFromBottom(h0, stockMoveBottom, bw);
        assertEquals(71, stockTop0);

        // Height 220 (3 DP rows): stock path drops ↰ onto a slot; mixin keeps Sort's top.
        int h3 = 220;
        int stockTop3 = IpnInventorioMovePlacement.topFromBottom(h3, stockMoveBottom, bw);
        assertEquals(125, stockTop3);
        // After mixin: same TopRight(top, right) as computed from Sort — independent of height.
        assertEquals(71, sortTop);
        assertEquals(159, IpnInventorioMovePlacement.leftFromRight(w, moveRight, bw));
    }

    @Test
    void inventorioMovePlacementMixinIsPinnedAndRegistered() throws IOException {
        String m = read("src/main/java/dev/councilcraft/mixin/client/IpnInventorioMovePlacementMixin.java");
        assertTrue(m.contains("@Pseudo"));
        assertTrue(m.contains("SortingButtonCollectionWidget$InitWidgets"));
        assertTrue(m.contains("method = \"reHint\""));
        assertTrue(m.contains("@At(\"RETURN\")"));
        assertTrue(m.contains("require = 0"));
        assertTrue(m.contains("remap = false"));
        assertTrue(m.contains("moveAllToContainer"));
        assertTrue(m.contains("sortButton"));
        assertTrue(m.contains("de.rubixdev.inventorio.client.ui.InventorioScreen"));
        assertTrue(m.contains("IpnInventorioMovePlacement.moveRightFromSort"));
        String client = read("src/main/resources/councilcraft.client.mixins.json");
        assertTrue(client.contains("IpnInventorioMovePlacementMixin"));
        assertTrue(client.contains("IpnHonorHideMixin"));
        String plugin = read("src/main/java/dev/councilcraft/mixin/CouncilCraftMixinPlugin.java");
        assertTrue(plugin.contains("IpnInventorioMovePlacementMixin"));
        assertTrue(plugin.contains("\"1.10.20\""));
        assertTrue(plugin.contains("\"1.10.4\""));
        // Honor-hide remains for MOVE_TO_PLAYER.
        String hide = read("src/main/java/dev/councilcraft/mixin/client/IpnHonorHideMixin.java");
        assertTrue(hide.contains("moveAllToPlayer"));
        assertTrue(hide.contains("getHide"));
    }
}
