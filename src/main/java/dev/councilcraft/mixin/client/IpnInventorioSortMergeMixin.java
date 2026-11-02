package dev.councilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.councilcraft.compat.client.IpnInventorioSortMerge;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * CC-B23. Wrap IPN sort entry points so Inventorio screens merge player main storage into the
 * container sort area (see {@link IpnInventorioSortMerge} + {@code IpnItemStorageBeltMixin}).
 *
 * <p>Targets GeneralInventoryActions (not AreaTypes nested AreaType classes) to avoid the
 * clinit/NoClassDefFound failure documented for PT88.
 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.GeneralInventoryActions", remap = false)
public class IpnInventorioSortMergeMixin {
    @WrapMethod(method = "doSort", remap = false, require = 0)
    private void councilcraft$mergeSort(ScreenHandler handler, boolean chat, Operation<Void> original) {
        IpnInventorioSortMerge.runMergingPlayerStorage(handler, () -> original.call(handler, chat));
    }

    @WrapMethod(method = "doSortInColumns", remap = false, require = 0)
    private void councilcraft$mergeSortColumns(ScreenHandler handler, boolean chat, Operation<Void> original) {
        IpnInventorioSortMerge.runMergingPlayerStorage(handler, () -> original.call(handler, chat));
    }

    @WrapMethod(method = "doSortInRows", remap = false, require = 0)
    private void councilcraft$mergeSortRows(ScreenHandler handler, boolean chat, Operation<Void> original) {
        IpnInventorioSortMerge.runMergingPlayerStorage(handler, () -> original.call(handler, chat));
    }
}
