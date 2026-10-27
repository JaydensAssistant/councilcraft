package dev.councilcraft.mixin.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT86. IPN 1.10.20 only consults ButtonPositionHint.hide when creating PROFILE_SELECTOR.
 * MOVE_TO_CONTAINER / MOVE_TO_PLAYER still call setVisible(moveAllVisible) after setHints.
 * After InitWidgets finishes (hints applied, visibility set, reHint run), force those two
 * buttons invisible when their hint says hide — so InventorioScreen can ship hide:true for
 * MOVE_TO_CONTAINER while chests keep global SHOW_MOVE_ALL_BUTTON.
 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.gui.inject.SortingButtonCollectionWidget$InitWidgets", remap = false)
public class IpnHonorHideMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 0)
    private void councilcraft$honorHide(CallbackInfo ci) {
        councilcraft$hideIfHinted("moveAllToContainer");
        councilcraft$hideIfHinted("moveAllToPlayer");
    }

    private void councilcraft$hideIfHinted(String fieldName) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object button = field.get(this);
            if (button == null) {
                return;
            }
            Object hints = button.getClass().getField("hints").get(button);
            if (hints == null) {
                return;
            }
            Object hide = hints.getClass().getMethod("getHide").invoke(hints);
            if (!Boolean.TRUE.equals(hide)) {
                return;
            }
            Method setVisible = null;
            Class<?> c = button.getClass();
            while (c != null && setVisible == null) {
                try {
                    setVisible = c.getDeclaredMethod("setVisible", boolean.class);
                } catch (NoSuchMethodException e) {
                    c = c.getSuperclass();
                }
            }
            if (setVisible != null) {
                setVisible.setAccessible(true);
                setVisible.invoke(button, false);
            }
        } catch (ReflectiveOperationException ignored) {
            // Shape changed — pin skips via require=0 / plugin
        }
    }
}
