package dev.councilcraft.item;

import dev.councilcraft.CouncilCraftRules;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * PM25. A real Antique Atlas item, replacing Antique Atlas's "book renamed Antique Atlas" convention.
 * Stack 1, not damageable, no tags, so it takes no enchantments. Antique Atlas itself is client-only;
 * our client mixin (AntiqueAtlasItemMixin) makes it recognise this item and nothing else. The item model
 * uses Antique Atlas's own model (its textures are CC BY-NC-SA, so nothing is copied). Its use does
 * nothing on its own: Antique Atlas opens the map from Item.use's PASS result on the client.
 */
public final class AntiqueAtlasItem {
    public static final Item ATLAS = new Item(new Item.Settings().maxCount(1));

    private AntiqueAtlasItem() {
    }

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(CouncilCraftRules.MOD_ID, "antique_atlas"), ATLAS);
        // No creative tab entry here: Antique Atlas adds its handheld atlas after the filled map in
        // Tools & Utilities, and the client mixin makes that entry this item.
    }
}
