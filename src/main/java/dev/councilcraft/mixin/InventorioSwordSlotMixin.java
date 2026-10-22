package dev.councilcraft.mixin;

import de.rubixdev.inventorio.api.ToolBeltSlotTemplate;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import dev.councilcraft.compat.InventorioToolBelt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM68. Inventorio's tool belt loses its sword slot: pickaxe, axe, shovel, hoe. Every belt size
 * (screen layout, inventory size, slot ids) is read from this template list, so removing the
 * template right after Inventorio registers it is the whole change. The same code runs on client
 * and server, so both agree on the slot count. The template object is still returned, so
 * Inventorio's registration chain (allowing conditions and tags) runs on it harmlessly.
 * Applied only with Inventorio.
 */
@Mixin(value = PlayerInventoryAddon.Companion.class, remap = false)
public abstract class InventorioSwordSlotMixin {
    @Inject(method = "registerToolBeltTemplateIfNotExists", at = @At("RETURN"))
    private void councilcraft$dropSwordSlot(String slotName, ToolBeltSlotTemplate template, CallbackInfoReturnable<ToolBeltSlotTemplate> cir) {
        if (InventorioToolBelt.SLOT_SWORD.equals(slotName)) {
            ((PlayerInventoryAddon.Companion) (Object) this).getToolBeltTemplates$inventorio_mc1_20_1_fabric()
                .removeIf(t -> InventorioToolBelt.SLOT_SWORD.equals(t.getName()));
        }
    }
}
