package dev.councilcraft.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.councilcraft.compat.FlagBannerNbt;
import dev.councilcraft.compat.FlagBanners;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 0.4.21: what unit tests can prove without a world for PT51, PT52 and PT53. */
class Release0421RulesTest {
    private static final String ELITE = "pack/shaderpacks/E-LITE shaders 5.1.3 CouncilCraft.zip";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    private static String read(String p) throws IOException {
        return Files.readString(Path.of(p));
    }

    private static String zipText(String entry) throws IOException {
        try (ZipFile z = new ZipFile(Path.of(ELITE).toFile())) {
            ZipEntry e = z.getEntry(entry);
            assertTrue(e != null, entry + " is in the shader zip");
            return new String(z.getInputStream(e).readAllBytes(), "UTF-8");
        }
    }

    @Test
    void versionIsAtLeast0421() throws IOException {
        assertTrue(read("gradle.properties").matches("(?s).*mod_version=0\\.4\\.(2[1-9]|[3-9]\\d).*"));
        assertTrue(read("pack/pack.toml").matches("(?s).*version = \"0\\.4\\.(2[1-9]|[3-9]\\d)\".*"));
    }

    // PT51 -------------------------------------------------------------------------------------------------------

    @Test
    void netherSkyPatchIsAppliedByMkelite() throws IOException {
        assertTrue(Files.exists(Path.of("docs/patches/elite-5.1.3-councilcraft-nether-dh-sky.diff")));
        String tool = read("pack/tools/mkelite.py");
        assertTrue(tool.contains("elite-5.1.3-councilcraft-taa.diff"));
        assertTrue(tool.contains("elite-5.1.3-councilcraft-nether-dh-sky.diff"));
    }

    @Test
    void shaderZipPaintsNetherSkyWithDhOnlyWhereBothDepthsAreFar() throws IOException {
        String g = zipText("shaders/common/deferred_fragment.glsl");
        // Stock branch (no DH) is unchanged.
        assertTrue(g.contains("#if !defined DISTANT_HORIZONS\n                if(linear_d > 0.9999) {  // Only sky"));
        // New DH branch: vanilla depth AND dhDepthTex0 at the far plane, same color as the terrain fog.
        int i = g.indexOf("#else\n                // CouncilCraft patch (PT51)");
        assertTrue(i > 0, "PT51 branch present");
        String branch = g.substring(i, g.indexOf("#endif", i));
        assertTrue(branch.contains("linear_d > 0.9999 && d2 >= 1.0"));
        assertTrue(branch.contains("vec4(mix(fogColor * 0.5, vec3(1.0), -0.025), 1.0)"));
        // d2 is the dhDepthTex0 sample and dhDepthTex0 is declared under DISTANT_HORIZONS.
        assertTrue(g.contains("float d2 = texture2DLod(dhDepthTex0, texcoord * RENDER_SCALE, 0).r;"));
        assertTrue(g.contains("#if defined DISTANT_HORIZONS\n    uniform sampler2D dhDepthTex0;"));
    }

    @Test
    void lgplNoticeListsTheNetherSkyEdit() throws IOException {
        String c = zipText("CHANGES-CouncilCraft.txt");
        assertTrue(c.contains("shaders/common/deferred_fragment.glsl"));
        assertTrue(c.contains("elite-5.1.3-councilcraft-nether-dh-sky.diff"));
        assertTrue(c.contains("dhDepthTex0"));
        assertTrue(zipText("shaders/lib/fast_taa.glsl").contains("CouncilCraft patch"), "TAA patch still applied");
    }

    // PT52 -------------------------------------------------------------------------------------------------------

    @Test
    void nightBrightIs085AndSettingsStaySorted() throws IOException {
        List<String> lines = new ArrayList<>(Files.readAllLines(Path.of(ELITE + ".txt")));
        lines.removeIf(String::isBlank);
        long n = lines.stream().filter(l -> l.startsWith("NIGHT_BRIGHT=")).count();
        assertEquals(1, n);
        assertTrue(lines.contains("NIGHT_BRIGHT=0.85"));
        List<String> keys = new ArrayList<>();
        for (String l : lines) keys.add(l.substring(0, l.indexOf('=')));
        List<String> sorted = new ArrayList<>(keys);
        sorted.sort(null);
        assertEquals(sorted, keys, "settings file keeps its sorted order");
        assertFalse(keys.isEmpty());
    }

    // PT53 -------------------------------------------------------------------------------------------------------

    private static NbtCompound patterned() {
        NbtCompound p = new NbtCompound();
        p.putString("Pattern", "bts");
        p.putInt("Color", 11);
        NbtList list = new NbtList();
        list.add(p);
        NbtCompound bet = new NbtCompound();
        bet.put("Patterns", list);
        NbtCompound tag = new NbtCompound();
        tag.put("BlockEntityTag", bet);
        return tag;
    }

    /** What the funnel gets when 16 flags are held: the marked banner form, count 16 (stick stands in for the flag item). */
    private static ItemStack markedFlags16() {
        ItemStack s = new ItemStack(Items.RED_BANNER, 16);
        s.setNbt(FlagBannerNbt.mark(patterned(), "minecraft:stick"));
        return s;
    }

    @Test
    void flagFromAStackOf16IsStoredAndReturnedAsOneWithIdenticalNbt() {
        ItemStack held = markedFlags16();
        ItemStack stored = (ItemStack) FlagBanners.forShipSlot(held);
        assertEquals(1, stored.getCount());
        assertEquals(16, held.getCount(), "the player's stack is never touched (Small Ships decrements it itself)");
        // R1 detach: giveBack(stored.copy()).
        ItemStack back = FlagBanners.handBack(stored.copy());
        assertSame(Items.STICK, back.getItem());
        assertEquals(1, back.getCount());
        assertEquals(patterned(), back.getNbt());
    }

    @Test
    void vanillaBannerFromAStackOf16IsStoredAndReturnedAsOne() {
        ItemStack held = new ItemStack(Items.RED_BANNER, 16);
        held.setNbt(patterned());
        ItemStack stored = (ItemStack) FlagBanners.forShipSlot(held);
        assertEquals(1, stored.getCount());
        assertEquals(16, held.getCount());
        assertEquals(patterned(), stored.getNbt());
        ItemStack back = FlagBanners.handBack(stored.copy());
        assertSame(Items.RED_BANNER, back.getItem());
        assertEquals(1, back.getCount());
        assertEquals(patterned(), back.getNbt());
    }

    @Test
    void replacePathDropsOneEvenForAShipSavedWithAStack() {
        // R2: interactBanner's getData result goes through forHandBack, then Small Ships drops it.
        ItemStack legacyFlag = (ItemStack) FlagBanners.forHandBack(markedFlags16());
        assertSame(Items.STICK, legacyFlag.getItem());
        assertEquals(1, legacyFlag.getCount());
        assertEquals(patterned(), legacyFlag.getNbt());
        ItemStack legacyVanilla = new ItemStack(Items.BLUE_BANNER, 12);
        ItemStack dropped = (ItemStack) FlagBanners.forHandBack(legacyVanilla);
        assertEquals(1, dropped.getCount());
        assertSame(Items.BLUE_BANNER, dropped.getItem());
    }

    @Test
    void shipSavedWithABiggerCountIsCorrectedOnLoadAndOnDetach() {
        // Loading saved data writes through setData (the funnel); a detach also clamps on its own.
        ItemStack loaded = (ItemStack) FlagBanners.forShipSlot(markedFlags16());
        assertEquals(1, loaded.getCount());
        assertEquals(1, FlagBanners.handBack(markedFlags16()).getCount());
        assertEquals(1, FlagBanners.handBack(new ItemStack(Items.WHITE_BANNER, 64)).getCount());
    }

    @Test
    void dockyardCostsAndRefundsAreAlreadyOneAndStayIdentical() {
        // Small Ships' Dockyard costs and refunds copyWithCount(1) and installs a count-1 copy; both are funneled.
        ItemStack cost = markedFlags16().copyWithCount(1);
        ItemStack installed = (ItemStack) FlagBanners.forShipSlot(cost.copy());
        assertEquals(1, installed.getCount());
        ItemStack refund = FlagBanners.restore(installed.copyWithCount(1));
        assertSame(Items.STICK, refund.getItem());
        assertEquals(1, refund.getCount());
        assertEquals(patterned(), refund.getNbt());
    }

    @Test
    void emptyAndSingleStacksAreUnchanged() {
        assertSame(ItemStack.EMPTY, FlagBanners.forShipSlot(ItemStack.EMPTY));
        ItemStack one = new ItemStack(Items.RED_BANNER, 1);
        assertSame(one, FlagBanners.forShipSlot(one));
        assertEquals(ItemStack.EMPTY, FlagBanners.handBack(ItemStack.EMPTY));
        assertEquals("keep", FlagBanners.forShipSlot("keep"));
        assertEquals(7, FlagBanners.forShipSlot(7));
    }

    @Test
    void detachMixinGivesBackThroughHandBack() throws IOException {
        assertTrue(read("src/main/java/dev/councilcraft/mixin/ShipDetachFlagMixin.java").contains("FlagBanners.handBack(stack)"));
        assertTrue(read("src/main/java/dev/councilcraft/mixin/ShipFlagBannerMixin.java").contains("FlagBanners.forShipSlot(value)"));
    }
}
