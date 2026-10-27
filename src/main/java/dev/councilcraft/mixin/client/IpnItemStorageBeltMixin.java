package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.InventorioBeltSlots;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT76 note. Container-style sort path (itemStorage / fillSlots) skips disabled$1, so filter
 * belt and crafting indices out of the returned ItemArea's slotIndices after getItemArea.
 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.AreaTypes$itemStorage$1", remap = false)
public class IpnItemStorageBeltMixin {
    @Inject(method = "getItemArea", at = @At("RETURN"), remap = false, require = 0)
    private void councilcraft$filterBelts(ScreenHandler handler, List<Slot> slots, CallbackInfoReturnable<Object> cir) {
        Object area = cir.getReturnValue();
        if (area == null || slots == null || slots.isEmpty()) {
            return;
        }
        try {
            Method get = area.getClass().getMethod("getSlotIndices");
            @SuppressWarnings("unchecked")
            List<Object> indices = (List<Object>) get.invoke(area);
            if (indices == null || indices.isEmpty()) {
                return;
            }
            indices.removeIf(idx -> {
                int i = ((Number) idx).intValue();
                if (i < 0 || i >= slots.size()) {
                    return false;
                }
                return InventorioBeltSlots.isExcludedFromContainerSort(slots.get(i));
            });
        } catch (ReflectiveOperationException ignored) {
            // ItemArea shape changed — pin skips via require=0 / plugin
        }
    }
}
