package dev.councilcraft.coin;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.CoinBreakdown;
import dev.councilcraft.rules.CoinMoney;
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
 *
 * <p>PPM8. Prices, balances and limits show one money total ({@link CoinMoney}): "N¢" under 100,
 * otherwise "$D.CC". The andesite depositor's price is a plain coin count times the coin's value.
 */
public final class NumismaticsText {
    static final Set<String> REFERENCE_PRICES = Set.of(
        "block.numismatics.vendor.tooltip.price",
        "block.numismatics.brass_depositor.tooltip.price",
        "gui.numismatics.salepoint.price",
        "gui.numismatics.bank_terminal.balance"
    );
    static final String ANDESITE_PRICE = "block.numismatics.andesite_depositor.tooltip.price";
    static final String LIMIT = "gui.numismatics.limit.monetary";
    static final String COIN_VALUE = "item.numismatics.coin.tooltip.value";
    static final String COIN_VALUE_BASIC = "item.numismatics.coin.tooltip.value.basic";

    private NumismaticsText() {
    }

    /** Text.translatable(key, args) with the pack's argument form for the keys above; any other key is untouched. */
    public static MutableText translatable(String key, Object[] args) {
        if (REFERENCE_PRICES.contains(key) && args.length == 3) {
            return Text.translatable(key, money(CoinBreakdown.total(args[0], CouncilCraftRules.SPUR_VALUE, args[2])));
        }
        if (ANDESITE_PRICE.equals(key) && args.length == 3) {
            return Text.translatable(key,
                money((long) CoinBreakdown.parseCount(args[0]) * CoinBreakdown.parseCount(args[2])));
        }
        if (LIMIT.equals(key) && args.length == 6) {
            return Text.translatable(key,
                money(CoinBreakdown.total(args[0], CouncilCraftRules.COG_VALUE, args[2])),
                money(CoinBreakdown.total(args[3], CouncilCraftRules.COG_VALUE, args[5])));
        }
        if (COIN_VALUE.equals(key) && args.length == 3) {
            return Text.translatable(key, diamonds(CoinBreakdown.parseCount(args[2])));
        }
        if (COIN_VALUE_BASIC.equals(key) && args.length == 1) {
            return Text.translatable(key, diamonds(CoinBreakdown.parseCount(args[0])));
        }
        return Text.translatable(key, args);
    }

    /** PPM8. "99¢", "$1.06", "$123.45". */
    public static MutableText money(long total) {
        return Text.literal(CoinMoney.format(total));
    }

    /** "1 Diamond" / "25 Diamonds". */
    public static MutableText diamonds(int count) {
        return Text.translatable(count == 1 ? "councilcraft.coins.diamond" : "councilcraft.coins.diamonds", count);
    }
}
