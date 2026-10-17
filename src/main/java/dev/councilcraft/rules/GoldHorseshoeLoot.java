package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;
import java.util.ArrayList;
import java.util.List;

/**
 * S245. Gold horseshoes are stripped from every Horseshoes loot injection.
 *
 * <p>Horseshoes 1.0.3 adds gold on its own to nether-bridge and all four bastion
 * chests, and adds gold beside iron and diamond in desert-pyramid and end-city
 * chests. The hook text names bastion and nether bridge because those pools are
 * gold-only. S245 says gold horseshoes never exist, so the mixed pools lose the
 * gold entry too. Iron and diamond injections are left in place.
 */
public final class GoldHorseshoeLoot {
    private GoldHorseshoeLoot() {
    }

    public static boolean isGoldHorseshoe(String itemId) {
        return CouncilCraftRules.GOLD_HORSESHOE_ID.equals(itemId);
    }

    /** Entries that remain. An empty result means the whole pool is not added. */
    public static List<String> withoutGold(List<String> itemIds) {
        List<String> kept = new ArrayList<>();
        for (String itemId : itemIds) {
            if (!isGoldHorseshoe(itemId)) {
                kept.add(itemId);
            }
        }
        return kept;
    }
}
