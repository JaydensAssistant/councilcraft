package dev.councilcraft.item;

import dev.councilcraft.CouncilCraft;
import dev.councilcraft.CouncilCraftRules;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * S141 / S173 / S242. Six government molds. None of them has a crafting recipe.
 * They are given out, not crafted. Each is fireproof and does not stack.
 *
 * <p>Death drops them, because nothing here marks them soulbound. Another player
 * can pick one up. {@code ItemEntityMoldMixin} stops lava, cactus, explosions,
 * the void and the 5-minute despawn from destroying the dropped item.
 */
public final class GovernmentMolds {
    public static final String[] NAMES = {"spur", "bevel", "sprocket", "cog", "crown", "sun"};

    private GovernmentMolds() {
    }

    public static void register() {
        for (String name : NAMES) {
            Item mold = new GovernmentMoldItem(new Item.Settings().maxCount(1).fireproof());
            Registry.register(Registries.ITEM, new Identifier(CouncilCraftRules.MOD_ID, name + "_mold"), mold);
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(mold));
        }
        CouncilCraft.LOGGER.info("Registered {} government molds.", NAMES.length);
    }
}
