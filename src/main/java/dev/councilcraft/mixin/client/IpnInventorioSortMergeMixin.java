package dev.councilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.councilcraft.compat.client.IpnInventorioSortMerge;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * CC-B23A / CC-B23AA. Wrap IPN sort entry points so Inventorio screens run Deep Pockets then main
 * inventory sorts separately (see {@link IpnInventorioSortMerge} + {@code IpnItemStorageBeltMixin}).
 *
 * <p>Targets GeneralInventoryActions (not AreaTypes nested AreaType classes) to avoid the
 * clinit/NoClassDefFound failure documented for PT88.
 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.GeneralInventoryActions", remap = false)
public class IpnInventorioSortMergeMixin {
    @WrapMethod(method = "doSort", remap = false, require = 0)
    private void councilcraft$separateSort(ScreenHandler handler, boolean chat, Operation<Void> original) {
        IpnInventorioSortMerge.runSeparatePlayerSorts(handler, () -> original.call(handler, chat));
    }

    @WrapMethod(method = "doSortInColumns", remap = false, require = 0)
    private void councilcraft$separateSortColumns(ScreenHandler handler, boolean chat, Operation<Void> original) {
        IpnInventorioSortMerge.runSeparatePlayerSorts(handler, () -> original.call(handler, chat));
    }

    @WrapMethod(method = "doSortInRows", remap = false, require = 0)
    private void councilcraft$separateSortRows(ScreenHandler handler, boolean chat, Operation<Void> original) {
        IpnInventorioSortMerge.runSeparatePlayerSorts(handler, () -> original.call(handler, chat));
    }
}
