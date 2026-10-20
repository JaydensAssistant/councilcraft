package dev.ithundxr.createnumismatics.content.coins;

import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

/**
 * Compile-time stand-in for Numismatics 1.1.0's CoinItem (Numismatics is not on the compile classpath).
 * Only the static method councilcraft calls. Not packed into our jar.
 */
public final class CoinItem {
    private CoinItem() {
    }

    public static boolean extract(PlayerEntity player, Hand hand, Map<?, Integer> coins, boolean onlyHand) {
        throw new UnsupportedOperationException("stub");
    }
}
