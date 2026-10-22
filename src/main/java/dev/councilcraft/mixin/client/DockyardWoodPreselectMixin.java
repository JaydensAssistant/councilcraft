package dev.councilcraft.mixin.client;

import com.talhanation.smallships.client.gui.screens.inventory.DockyardScreen;
import dev.councilcraft.rules.WoodLookMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM75. When the Small Ships 2.0.1 dockyard opens, preselect the boat wood that the planks the player holds the
 * most of map to (PPM73, {@link WoodLookMap#SHIP}); RU and painted planks pick their closest vanilla wood. Once per
 * opened screen, so a resize keeps the player's own choice, and the player can still change it. Client-only: the
 * server charges planks exactly as before. Applied only with Small Ships.
 */
@Mixin(DockyardScreen.class)
public abstract class DockyardWoodPreselectMixin {
    @Unique
    private boolean councilcraft$preselected;

    @Shadow(remap = false)
    private void setWoodType(BoatEntity.Type type) {
    }

    @Inject(method = "init", at = @At("TAIL"), require = 1)
    private void councilcraft$preselectWood(CallbackInfo ci) {
        if (this.councilcraft$preselected) {
            return;
        }
        this.councilcraft$preselected = true;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                counts.merge(Registries.ITEM.getId(stack.getItem()).toString(), stack.getCount(), Integer::sum);
            }
        }
        WoodLookMap.preselect(counts).ifPresent(wood -> this.setWoodType(BoatEntity.Type.valueOf(wood.toUpperCase(Locale.ROOT))));
    }
}
