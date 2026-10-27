package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.talhanation.smallships.world.entity.ship.Ship;
import dev.councilcraft.compat.FlagBanners;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PT43. The one place a banner reaches a Small Ships 2.0.1 ship: {@code Ship.setData}. A Supplementaries flag
 * written to the BANNER or SAIL_BANNER slot is stored as the marked vanilla banner (same colour, same patterns), so
 * every Small Ships path that sets a banner (right-click, Dockyard, the Dockyard preview, loading saved data)
 * ends up with a banner it can render. PT53: a ship holds ONE banner, so any non-empty stack written to those slots
 * (flag or vanilla banner) is stored with count 1 (Small Ships copies the whole held stack); an empty stack stays
 * empty, and a ship saved with a bigger count is corrected as it loads. Other data is written unchanged. Pinned to Small Ships 2.0.1 and
 * Supplementaries 1.20-3.1.44.
 */
@Mixin(value = Ship.class, remap = false)
public abstract class ShipFlagBannerMixin {
    @WrapOperation(method = "setData", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/data/DataTracker;set(Lnet/minecraft/entity/data/TrackedData;Ljava/lang/Object;)V",
        remap = true), require = 1)
    private void councilcraft$flagToBanner(DataTracker tracker, TrackedData<Object> key, Object value, Operation<Void> original) {
        Object slotKey = key;
        boolean bannerSlot = slotKey == Ship.BANNER || slotKey == Ship.SAIL_BANNER;
        original.call(tracker, key, bannerSlot ? FlagBanners.forShipSlot(value) : value);
    }
}
