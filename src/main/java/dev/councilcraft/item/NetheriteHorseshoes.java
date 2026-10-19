package dev.councilcraft.item;

import dev.councilcraft.CouncilCraftRules;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.hyper_pigeon.horseshoes.items.HorseshoesItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * S230. A {@link HorseshoesItem}, so the Horseshoes slot, the armor and speed
 * modifiers, and the feet-enchantment check all treat it as shoes. The texture
 * is a recolor of the diamond horseshoes. The smithing recipe is data, not code.
 * Fireproof, so lava does not destroy the item.
 */
public final class NetheriteHorseshoes {
    public static final HorseshoesItem NETHERITE = new HorseshoesItem(
        CouncilCraftRules.NETHERITE_HORSESHOE_SPEED,
        CouncilCraftRules.NETHERITE_HORSESHOE_ARMOR,
        new Item.Settings().maxCount(1).fireproof(),
        new Identifier(CouncilCraftRules.MOD_ID, "textures/item/netherite_horseshoes.png")
    );

    private NetheriteHorseshoes() {
    }

    public static void register() {
        Registry.register(
            Registries.ITEM,
            new Identifier(CouncilCraftRules.MOD_ID, "netherite_horseshoes"),
            NETHERITE
        );
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(NETHERITE));
    }
}
