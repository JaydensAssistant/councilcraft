package dev.councilcraft.coin;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.CoinBreakdown;
import java.util.List;
import java.util.Set;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * PM32 / PM32a / PM32b. Rewrites the arguments of Numismatics' price, balance, limit and coin-value strings.
 * The pack's Numismatics lang (kubejs/assets/numismatics/lang/en_us.json) gives those keys a single
 * {@code %s} (two for the limit), so the "N¤" leftover never shows.
 *
 * <p>Numismatics' UI reference coin is the pack config {@code defaultCoin = "SPUR"} (numismatics-common.toml),
 * so the reference value is 1 there; spending limits always use the cog (the 25 Diamond Quarter).
 */
public final class NumismaticsText {
    static final Set<String> REFERENCE_PRICES = Set.of(
        "block.numismatics.vendor.tooltip.price",
        "block.numismatics.brass_depositor.tooltip.price",
        "gui.numismatics.salepoint.price",
        "gui.numismatics.bank_terminal.balance"
    );
    static final String LIMIT = "gui.numismatics.limit.monetary";
    static final String COIN_VALUE = "item.numismatics.coin.tooltip.value";
    static final String COIN_VALUE_BASIC = "item.numismatics.coin.tooltip.value.basic";

    private NumismaticsText() {
    }

    /** Text.translatable(key, args) with the pack's argument form for the keys above; any other key is untouched. */
    public static MutableText translatable(String key, Object[] args) {
        if (REFERENCE_PRICES.contains(key) && args.length == 3) {
            return Text.translatable(key, coins(CoinBreakdown.total(args[0], CouncilCraftRules.SPUR_VALUE, args[2])));
        }
        if (LIMIT.equals(key) && args.length == 6) {
            return Text.translatable(key,
                coins(CoinBreakdown.total(args[0], CouncilCraftRules.COG_VALUE, args[2])),
                coins(CoinBreakdown.total(args[3], CouncilCraftRules.COG_VALUE, args[5])));
        }
        if (COIN_VALUE.equals(key) && args.length == 3) {
            return Text.translatable(key, diamonds(CoinBreakdown.parseCount(args[2])));
        }
        if (COIN_VALUE_BASIC.equals(key) && args.length == 1) {
            return Text.translatable(key, diamonds(CoinBreakdown.parseCount(args[0])));
        }
        return Text.translatable(key, args);
    }

    /** "3 Diamond Quarters, 2 Diamond Dimes"; "0 Diamonds" for nothing. */
    public static MutableText coins(int total) {
        List<CoinBreakdown.Part> parts = CoinBreakdown.split(total);
        if (parts.isEmpty()) {
            return diamonds(0);
        }
        MutableText out = Text.empty();
        for (int i = 0; i < parts.size(); i++) {
            CoinBreakdown.Part part = parts.get(i);
            if (i > 0) {
                out.append(Text.literal(", "));
            }
            String name = "item.numismatics." + part.coin() + (part.count() == 1 ? "" : ".plural");
            out.append(Text.literal(part.count() + " ")).append(Text.translatable(name));
        }
        return out;
    }

    /** "1 Diamond" / "25 Diamonds". */
    public static MutableText diamonds(int count) {
        return Text.translatable(count == 1 ? "councilcraft.coins.diamond" : "councilcraft.coins.diamonds", count);
    }
}
