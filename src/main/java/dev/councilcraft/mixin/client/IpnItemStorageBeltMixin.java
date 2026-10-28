package dev.councilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.councilcraft.compat.InventorioBeltSlots;
import java.util.List;
import java.util.Set;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * PT76 / PT85 / PT88. After AreaTypes.fillSlots builds the container-area index list (every slot whose
 * inventory is not PlayerInventory), drop Inventorio tool/utility belt and crafting indices so
 * container-style sort never touches them.
 *
 * <p>PT85 targeted fillSlots at RETURN, but IPN 1.10.20's compiled fillSlots reuses parameter slots
 * (local 1 becomes an int, local 2 an Iterator), so a RETURN injector that reloads args from locals
 * 1–3 fails verification with "Bad local variable type". PT88 replaces that with MixinExtras
 * {@code @WrapMethod}: the wrapper receives the original Java args, calls {@code original.call}, then
 * filters {@code out}. Targets fillSlots (not the itemStorage nested class loaded during AreaTypes
 * clinit), because a mid-clinit mixin on that nested class caused NoClassDefFoundError on every IPN
 * button under IPN 1.10.20.
 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.AreaTypes", remap = false)
public class IpnItemStorageBeltMixin {
    @WrapMethod(method = "fillSlots", remap = false, require = 0)
    private void councilcraft$filterBelts(Set<?> types, List<?> slots, List<?> out, Operation<Void> original) {
        original.call(types, slots, out);
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
