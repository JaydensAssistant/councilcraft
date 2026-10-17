package dev.councilcraft.rules;

import dev.councilcraft.CouncilCraftRules;
import java.util.Map;

/**
 * S123. Pack value for each Numismatics coin.
 *
 * <p>{@code BY_STOCK} is what the mixin writes over. The keys are the numbers
 * in Numismatics 1.1.0's {@code Coin} static setup, not the pack values.
 * SPUR is already 1, and that instruction is also another coin's ordinal, so
 * the mixin does not rewrite it. The map still records it.
 */
public final class CoinValues {
    private static final Map<Integer, Integer> BY_STOCK = Map.of(
        1, CouncilCraftRules.SPUR_VALUE,
        8, CouncilCraftRules.BEVEL_VALUE,
        16, CouncilCraftRules.SPROCKET_VALUE,
        64, CouncilCraftRules.COG_VALUE,
        512, CouncilCraftRules.CROWN_VALUE,
        4096, CouncilCraftRules.SUN_VALUE
    );

    private static final Map<String, Integer> BY_NAME = Map.of(
        "SPUR", CouncilCraftRules.SPUR_VALUE,
        "BEVEL", CouncilCraftRules.BEVEL_VALUE,
        "SPROCKET", CouncilCraftRules.SPROCKET_VALUE,
        "COG", CouncilCraftRules.COG_VALUE,
        "CROWN", CouncilCraftRules.CROWN_VALUE,
        "SUN", CouncilCraftRules.SUN_VALUE
    );

    private CoinValues() {
    }

    /** Pack value for a stock number. Throws if that number is not one of the six. */
    public static int replaceStock(int stock) {
        Integer pack = BY_STOCK.get(stock);
        if (pack == null) {
            throw new IllegalStateException(
                "Numismatics coin stock value " + stock + " is not 1, 8, 16, 64, 512 or 4096."
            );
        }
        return pack;
    }

    /** Pack value for an enum name, or null when the name is not one of the six. */
    public static Integer packValue(String enumName) {
        if (enumName == null) {
            return null;
        }
        return BY_NAME.get(enumName);
    }
}
