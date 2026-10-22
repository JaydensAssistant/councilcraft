package dev.councilcraft.compat;

import dev.councilcraft.rules.ParagliderHand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import tictim.paraglider.api.item.Paraglider;

/**
 * PPM70. The stack Paragliders should treat as "the paraglider in hand": the main hand when it holds
 * one, else the offhand ({@link ParagliderHand}). When neither does, the main-hand stack is returned
 * unchanged, so the mod's own {@code instanceof Paraglider} checks fail exactly as before.
 */
public final class ParagliderHands {
    private ParagliderHands() {
    }

    public static ItemStack glider(PlayerEntity player, ItemStack mainHand) {
        ItemStack offHand = player.getOffHandStack();
        return ParagliderHand.pick(mainHand.getItem() instanceof Paraglider, offHand.getItem() instanceof Paraglider)
            == ParagliderHand.Hand.OFF ? offHand : mainHand;
    }

    public static ItemStack glider(PlayerEntity player) {
        return glider(player, player.getMainHandStack());
    }
}
