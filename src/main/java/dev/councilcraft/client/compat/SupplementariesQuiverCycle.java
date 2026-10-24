package dev.councilcraft.client.compat;

import net.mehvahdjukaar.supplementaries.common.items.SelectableContainerItem;
import net.mehvahdjukaar.supplementaries.common.network.ModNetwork;
import net.mehvahdjukaar.supplementaries.common.network.ServerBoundCycleSelectableContainerItemPacket;
import net.mehvahdjukaar.supplementaries.common.utils.IQuiverPlayer;
import net.mehvahdjukaar.supplementaries.common.utils.SlotReference;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * PPM105. Supplementaries 3.1.44 quiver cycling, done the way its own quiver HUD does it
 * (SelectableContainerItemHud.sendCycle): send ServerBoundCycleSelectableContainerItemPacket(amount,
 * slot) through ModNetwork.CHANNEL, then cycle the client copy. The server handler cycles the
 * selected slot of the quiver at that SlotReference. Not a stable API: re-check the record (int amount,
 * SlotReference slotReference, boolean setSlot) with javap whenever Supplementaries is updated.
 * Separate class so Supplementaries types load only when this runs.
 */
public final class SupplementariesQuiverCycle {
    private SupplementariesQuiverCycle() {
    }

    /** Selects the next arrow in the player's active quiver; does nothing without one. */
    public static void next(ClientPlayerEntity player) {
        if (!(player instanceof IQuiverPlayer quiverPlayer)) {
            return;
        }
        SlotReference slot = quiverPlayer.supplementaries$getQuiverSlot();
        if (slot == null || slot.isEmpty()) {
            return;
        }
        ItemStack stack = slot.get(player);
        if (!(stack.getItem() instanceof SelectableContainerItem<?> item)) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new ServerBoundCycleSelectableContainerItemPacket(1, slot));
        item.getData(stack).cycle(1);
    }
}
