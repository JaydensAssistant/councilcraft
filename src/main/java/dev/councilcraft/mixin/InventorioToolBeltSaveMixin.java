package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import de.rubixdev.inventorio.player.PlayerAddonSerializer;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import dev.councilcraft.compat.InventorioToolBelt;
import dev.councilcraft.rules.ToolBeltMigration;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM68. One-time save migration for the 4-slot tool belt ({@link ToolBeltMigration}). Before
 * Inventorio reads an unmarked save, the belt list is moved down past the old sword slot and the
 * sword is taken out; after the read (the vanilla inventory is already loaded) it goes to the main
 * inventory. Every save is written with the marker, so this runs once per player.
 * Applied only with Inventorio.
 */
@Mixin(value = PlayerAddonSerializer.class, remap = false)
public abstract class InventorioToolBeltSaveMixin {
    @Inject(method = "deserialize", at = @At("HEAD"))
    private void councilcraft$migrateBelt(PlayerInventoryAddon addon, NbtCompound tag, CallbackInfo ci,
                                          @Share("swords") LocalRef<List<ItemStack>> swords) {
        swords.set(addon.getPlayer().getWorld().isClient ? List.of() : InventorioToolBelt.migrate(tag));
    }

    @Inject(method = "deserialize", at = @At("TAIL"))
    private void councilcraft$returnSword(PlayerInventoryAddon addon, NbtCompound tag, CallbackInfo ci,
                                          @Share("swords") LocalRef<List<ItemStack>> swords) {
        List<ItemStack> stacks = swords.get();
        if (stacks != null) {
            InventorioToolBelt.giveBack(addon.getPlayer(), stacks);
        }
    }

    @Inject(method = "serialize", at = @At("TAIL"))
    private void councilcraft$markBelt(PlayerInventoryAddon addon, NbtCompound tag, CallbackInfo ci) {
        tag.putInt(ToolBeltMigration.MARKER, ToolBeltMigration.VERSION);
    }
}
