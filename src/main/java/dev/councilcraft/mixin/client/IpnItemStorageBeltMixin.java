package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.InventorioBeltSlots;
import java.util.List;
import java.util.Set;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT76 / PT85. After AreaTypes.fillSlots builds the container-area index list (every slot whose
 * inventory is not PlayerInventory), drop Inventorio tool/utility belt and crafting indices so
 * container-style sort never touches them. Targets fillSlots at RETURN (not the itemStorage nested class loaded during AreaTypes clinit),
 * because a mid-clinit mixin on that nested class caused NoClassDefFoundError on every IPN button
 * under IPN 1.10.20.
 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.AreaTypes", remap = false)
public class IpnItemStorageBeltMixin {
    @Inject(method = "fillSlots", at = @At("RETURN"), remap = false, require = 0)
    private void councilcraft$filterBelts(Set<?> types, List<?> slots, List<?> out, CallbackInfo ci) {
        if (slots == null || out == null || out.isEmpty()) {
            return;
        }
        int size = slots.size();
        for (int i = out.size() - 1; i >= 0; i--) {
            Object raw = out.get(i);
            if (!(raw instanceof Number)) {
                continue;
            }
            int idx = ((Number) raw).intValue();
            if (idx < 0 || idx >= size) {
                continue;
            }
            Object slot = slots.get(idx);
            if (slot instanceof Slot && InventorioBeltSlots.isExcludedFromContainerSort((Slot) slot)) {
                out.remove(i);
            }
        }
    }
}
