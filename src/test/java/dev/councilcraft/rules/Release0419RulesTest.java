package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.compat.FlagBannerNbt;
import dev.councilcraft.compat.FlagBanners;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 0.4.19: PT43 (Supplementaries flags on Small Ships), what unit tests can prove without a world. */
class Release0419RulesTest {
    private static final String RED_FLAG = "supplementaries:flag_red";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    /** What a patterned, named flag's NBT looks like: BlockEntityTag.Patterns plus a display name. */
    private static NbtCompound patternedFlagNbt() {
        NbtCompound pattern1 = new NbtCompound();
        pattern1.putString("Pattern", "bts");
        pattern1.putInt("Color", 11);
        NbtCompound pattern2 = new NbtCompound();
        pattern2.putString("Pattern", "mc");
        pattern2.putInt("Color", 0);
        NbtList patterns = new NbtList();
        patterns.add(pattern1);
        patterns.add(pattern2);
        NbtCompound blockEntityTag = new NbtCompound();
        blockEntityTag.put("Patterns", patterns);
        NbtCompound display = new NbtCompound();
        display.putString("Name", "{\"text\":\"Council flag\"}");
        NbtCompound tag = new NbtCompound();
        tag.put("BlockEntityTag", blockEntityTag);
        tag.put("display", display);
        return tag;
    }

    @Test
    void versionIsAtLeast0419() throws IOException {
        // The exact version is asserted by the newest release test (Release0420RulesTest).
        assertTrue(read("gradle.properties").matches("(?s).*mod_version=0\\.4\\.(19|[2-9]\\d).*"));
    }

    // ---- the round trip: flag in, flag out with identical NBT ----

    @Test
    void patternedFlagNbtRoundTripsIdentically() {
        NbtCompound flag = patternedFlagNbt();
        NbtCompound onShip = FlagBannerNbt.mark(flag, RED_FLAG);
        assertEquals(RED_FLAG, FlagBannerNbt.originalId(onShip));
        assertNotEquals(flag, onShip);
        // The patterns Small Ships renders are untouched.
        assertEquals(flag.getCompound("BlockEntityTag"), onShip.getCompound("BlockEntityTag"));
        NbtCompound back = FlagBannerNbt.unmark(onShip);
        assertEquals(flag, back);
        assertEquals(flag.toString(), back.toString());
    }

    @Test
    void markingDoesNotChangeTheOriginal() {
        NbtCompound flag = patternedFlagNbt();
        NbtCompound before = flag.copy();
        FlagBannerNbt.mark(flag, RED_FLAG);
        assertEquals(before, flag);
        NbtCompound marked = FlagBannerNbt.mark(flag, RED_FLAG);
        NbtCompound markedBefore = marked.copy();
        FlagBannerNbt.unmark(marked);
        assertEquals(markedBefore, marked);
    }

    @Test
    void plainFlagWithoutNbtComesBackWithoutNbt() {
        NbtCompound onShip = FlagBannerNbt.mark(null, RED_FLAG);
        assertTrue(FlagBannerNbt.isMarked(onShip));
        assertNull(FlagBannerNbt.unmark(onShip));
    }

    @Test
    void markerSurvivesSavingAndLoadingTheShip() throws IOException {
        NbtCompound onShip = FlagBannerNbt.mark(patternedFlagNbt(), RED_FLAG);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.write(onShip, new DataOutputStream(bytes));
        NbtCompound loaded = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        assertEquals(onShip, loaded);
        assertEquals(patternedFlagNbt(), FlagBannerNbt.unmark(loaded));
    }

    @Test
    void unmarkedNbtIsNotMarked() {
        assertFalse(FlagBannerNbt.isMarked(null));
        assertFalse(FlagBannerNbt.isMarked(patternedFlagNbt()));
        assertNull(FlagBannerNbt.originalId(patternedFlagNbt()));
        NbtCompound wrongType = new NbtCompound();
        wrongType.putInt(FlagBannerNbt.MARKER, 1);
        assertFalse(FlagBannerNbt.isMarked(wrongType));
    }

    // ---- the stack level: restoring a marked banner, and leaving every other stack alone ----

    @Test
    void markedBannerBecomesTheOriginalItemWithIdenticalNbtAndCount() {
        // minecraft:stick stands in for the flag item: the restore only needs a registered item id.
        NbtCompound flag = patternedFlagNbt();
        ItemStack onShip = new ItemStack(Items.RED_BANNER, 1);
        onShip.setNbt(FlagBannerNbt.mark(flag, "minecraft:stick"));
        ItemStack back = FlagBanners.restore(onShip);
        assertSame(Items.STICK, back.getItem());
        assertEquals(1, back.getCount());
        assertEquals(flag, back.getNbt());
    }

    @Test
    void markedBannerWithoutOtherNbtBecomesAnItemWithoutNbt() {
        ItemStack onShip = new ItemStack(Items.WHITE_BANNER, 1);
        onShip.setNbt(FlagBannerNbt.mark(null, "minecraft:stick"));
        ItemStack back = FlagBanners.restore(onShip);
        assertSame(Items.STICK, back.getItem());
        assertFalse(back.hasNbt());
    }

    @Test
    void unmarkedBannersAreReturnedUnchanged() {
        ItemStack plain = new ItemStack(Items.RED_BANNER, 1);
        assertSame(plain, FlagBanners.restore(plain));
        ItemStack patterned = new ItemStack(Items.BLUE_BANNER, 1);
        patterned.setNbt(patternedFlagNbt());
        assertSame(patterned, FlagBanners.restore(patterned));
        assertSame(patterned, FlagBanners.forHandBack(patterned));
        assertSame(ItemStack.EMPTY, FlagBanners.restore(ItemStack.EMPTY));
        assertNull(FlagBanners.restore(null));
        Object notAStack = "x";
        assertSame(notAStack, FlagBanners.forHandBack(notAStack));
        assertSame(notAStack, FlagBanners.forShipSlot(notAStack));
    }

    @Test
    void aMarkerNamingAnUnknownItemNeverLosesTheStack() {
        ItemStack onShip = new ItemStack(Items.RED_BANNER, 1);
        onShip.setNbt(FlagBannerNbt.mark(null, "nope:not_an_item"));
        assertSame(onShip, FlagBanners.restore(onShip));
        ItemStack badId = new ItemStack(Items.RED_BANNER, 1);
        badId.setNbt(FlagBannerNbt.mark(null, "Not A Valid Id"));
        assertSame(badId, FlagBanners.restore(badId));
    }

    @Test
    void nonFlagStacksAreNotConverted() {
        ItemStack banner = new ItemStack(Items.RED_BANNER, 1);
        assertSame(banner, FlagBanners.toBanner(banner));
        assertFalse(FlagBanners.isFlag(banner));
        assertSame(banner, FlagBanners.forShipSlot(banner));
        assertNotNull(FlagBanners.forShipSlot(ItemStack.EMPTY));
    }

    // ---- wiring: the pins, the registrations and every enumerated hook ----

    @Test
    void pinsNeedBothModsAtTheirExactBuilds() {
        String[] mixins = {"ShipFlagBannerMixin", "BannerableFlagMixin", "ShipDetachFlagMixin", "DockyardFlagMixin",
            "client.DockyardScreenFlagMixin"};
        for (String m : mixins) {
            String name = "dev.councilcraft.mixin." + m;
            Map<String, String> ok = Map.of("smallships", "2.0.1", "supplementaries", "1.20-3.1.44");
            assertTrue(dev.councilcraft.mixin.PluginAccess.matches(name, ok), m);
            assertFalse(dev.councilcraft.mixin.PluginAccess.matches(name, Map.of("smallships", "2.0.2", "supplementaries", "1.20-3.1.44")), m);
            assertFalse(dev.councilcraft.mixin.PluginAccess.matches(name, Map.of("smallships", "2.0.1", "supplementaries", "1.20-3.1.45")), m);
            assertTrue(dev.councilcraft.mixin.PluginAccess.applies(name, java.util.Set.of("smallships", "supplementaries")), m);
            assertFalse(dev.councilcraft.mixin.PluginAccess.applies(name, java.util.Set.of("supplementaries")), m);
        }
    }

    @Test
    void everyFlagMixinIsRegistered() throws IOException {
        String common = read("src/main/resources/councilcraft.mixins.json");
        for (String m : new String[] {"ShipFlagBannerMixin", "BannerableFlagMixin", "ShipDetachFlagMixin", "DockyardFlagMixin"}) {
            assertTrue(common.contains("\"" + m + "\""), m);
        }
        assertTrue(read("src/main/resources/councilcraft.client.mixins.json").contains("\"DockyardScreenFlagMixin\""));
    }

    @Test
    void everyEnumeratedHookTargetsItsSmallShipsMethod() throws IOException {
        String store = read("src/main/java/dev/councilcraft/mixin/ShipFlagBannerMixin.java");
        assertTrue(store.contains("method = \"setData\""));
        assertTrue(store.contains("Ship.BANNER") && store.contains("Ship.SAIL_BANNER"));
        String bannerable = read("src/main/java/dev/councilcraft/mixin/BannerableFlagMixin.java");
        assertTrue(bannerable.contains("method = \"interactBanner\""));
        assertTrue(bannerable.contains("FlagBanners.toBanner") && bannerable.contains("FlagBanners.forHandBack"));
        String detach = read("src/main/java/dev/councilcraft/mixin/ShipDetachFlagMixin.java");
        assertTrue(detach.contains("method = \"giveBack\"") && detach.contains("FlagBanners.handBack"));
        String dock = read("src/main/java/dev/councilcraft/mixin/DockyardFlagMixin.java");
        assertTrue(dock.contains("method = \"startModifyTask\"") && dock.contains("FlagBanners.toBanner"));
        assertTrue(dock.contains("method = \"finishModify\"") && dock.contains("FlagBanners.restore"));
        String screen = read("src/main/java/dev/councilcraft/mixin/client/DockyardScreenFlagMixin.java");
        assertTrue(screen.contains("method = \"addBannerOptions\"") && screen.contains("method = \"buildOptionSignature\""));
        for (String src : new String[] {store, bannerable, detach, dock, screen}) {
            assertTrue(src.contains("require = 1"));
        }
    }

    @Test
    void theMarkerIsHiddenTopLevelNbt() {
        assertEquals("councilcraft_flag", FlagBannerNbt.MARKER);
        assertFalse(FlagBannerNbt.MARKER.contains("BlockEntityTag"));
        assertEquals(NbtElement.STRING_TYPE, FlagBannerNbt.mark(null, RED_FLAG).getType(FlagBannerNbt.MARKER));
    }
}
