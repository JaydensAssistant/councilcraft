package dev.councilcraft.mixin.client;

import de.rubixdev.inventorio.client.ui.InventorioScreen;
import de.rubixdev.inventorio.player.InventorioScreenHandler;
import java.util.ArrayList;
import java.util.List;
import kotlin.ranges.IntRange;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * PT83. On InventorioScreen, Mouse Tweaks findPushSlots / findPullSlot only see the vanilla
 * player inventory and available Deep Pockets — not crafting, tool belt, or utility belt.
 * Pinned to Mouse Tweaks 2.26 + Inventorio 1.10.4.
 */
@Pseudo
@Mixin(targets = "yalter.mousetweaks.Main", remap = false)
public class MouseTweaksDeepPocketsMixin {
    @ModifyVariable(method = "findPushSlots", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static List<Slot> councilcraft$filterPush(List<Slot> slots) {
        return filter(slots);
    }

    @ModifyVariable(method = "findPullSlot", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static List<Slot> councilcraft$filterPull(List<Slot> slots) {
        return filter(slots);
    }

    private static List<Slot> filter(List<Slot> slots) {
        if (slots == null || slots.isEmpty()) {
            return slots;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return slots;
        }
        Screen screen = client.currentScreen;
        if (!(screen instanceof InventorioScreen inventorio)) {
            return slots;
        }
        InventorioScreenHandler handler = inventorio.getScreenHandler();
        if (handler == null) {
            return slots;
        }
        IntRange deep = handler.getAvailableDeepPocketsRange();
        List<Slot> out = new ArrayList<>(slots.size());
        for (Slot slot : slots) {
            if (slot.inventory instanceof PlayerInventory) {
                out.add(slot);
                continue;
            }
            int id = slot.id;
            if (deep != null && id >= deep.getFirst() && id <= deep.getLast()) {
                out.add(slot);
            }
        }
        return out;
    }
}
