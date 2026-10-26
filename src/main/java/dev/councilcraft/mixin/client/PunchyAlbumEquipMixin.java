package dev.councilcraft.mixin.client;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT17. Exposure saves an album note on every keystroke (AlbumSyncNoteC2SP), which rewrites the held
 * album's NBT. Punchy 2.8g's HandEquipStateMachine.hasItemChanged compares NBT too, so it replayed its
 * equip animation (the hand bob) on every key. For exposure:album only, an NBT-only change is not an
 * item change. Slot switches go through Punchy's own hotbar check, and every other item (book and quill
 * included) keeps Punchy's stock comparison. Pinned to Punchy 2.8g in CouncilCraftMixinPlugin; another
 * Punchy build skips this mixin with one log line, and require = 0 keeps a renamed method from crashing.
 */
@Pseudo
@Mixin(targets = "punchy.client.state.HandEquipStateMachine")
public class PunchyAlbumEquipMixin {
    private static final Identifier COUNCILCRAFT$ALBUM = new Identifier("exposure", "album");

    @Inject(method = "hasItemChanged", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void councilcraft$albumNbtIsNotAChange(ItemStack previous, ItemStack current, CallbackInfoReturnable<Boolean> cir) {
        if (previous == null || current == null || previous.isEmpty() || current.isEmpty()) {
            return;
        }
        if (previous.isOf(current.getItem()) && COUNCILCRAFT$ALBUM.equals(Registries.ITEM.getId(current.getItem()))) {
            cir.setReturnValue(false);
        }
    }
}
