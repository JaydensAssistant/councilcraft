package dev.councilcraft.mixin.client;

import dev.councilcraft.compat.client.IpnInventorioMovePlacement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT90. After IPN's {@code InitWidgets.reHint} places buttons, pin MOVE_TO_CONTAINER next to
 * Sort on InventorioScreen only (same TopRight row, 12 px to Sort's right). Stock IPN uses
 * setBottomRight for MOVE on addChestSide screens, so Deep Pockets height growth drops ↰ onto
 * a slot. Chests are untouched. IpnHonorHideMixin still honors {@code hide} for MOVE_TO_PLAYER.
 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.gui.inject.SortingButtonCollectionWidget$InitWidgets", remap = false)
public class IpnInventorioMovePlacementMixin {
    private static final String INVENTORIO_SCREEN = "de.rubixdev.inventorio.client.ui.InventorioScreen";

    @Inject(method = "reHint", at = @At("RETURN"), remap = false, require = 0)
    private void councilcraft$pinMoveBesideSort(CallbackInfo ci) {
        try {
            if (!councilcraft$isInventorioScreen()) {
                return;
            }
            Object sort = councilcraft$field("sortButton");
            Object move = councilcraft$field("moveAllToContainer");
            if (sort == null || move == null) {
                return;
            }
            int sortTop = ((Number) councilcraft$call(sort, "getTop")).intValue();
            int sortRight = ((Number) councilcraft$call(sort, "getRight")).intValue();
            int moveRight = IpnInventorioMovePlacement.moveRightFromSort(sortRight);
            councilcraft$setTopRight(move, sortTop, moveRight);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Shape changed — pin skips via require=0 / plugin
        }
    }

    private boolean councilcraft$isInventorioScreen() throws ReflectiveOperationException {
        Field outer = this.getClass().getDeclaredField("this$0");
        outer.setAccessible(true);
        Object collection = outer.get(this);
        if (collection == null) {
            return false;
        }
        Method getScreen = null;
        Class<?> c = collection.getClass();
        while (c != null && getScreen == null) {
            try {
                getScreen = c.getDeclaredMethod("getScreen");
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        if (getScreen == null) {
            return false;
        }
        getScreen.setAccessible(true);
        Object screen = getScreen.invoke(collection);
        return screen != null && INVENTORIO_SCREEN.equals(screen.getClass().getName());
    }

    private Object councilcraft$field(String name) throws ReflectiveOperationException {
        Field field = this.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(this);
    }

    private static Object councilcraft$call(Object target, String method) throws ReflectiveOperationException {
        Method m = null;
        Class<?> c = target.getClass();
        while (c != null && m == null) {
            try {
                m = c.getDeclaredMethod(method);
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        if (m == null) {
            throw new NoSuchMethodException(method);
        }
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static void councilcraft$setTopRight(Object widget, int top, int right)
        throws ReflectiveOperationException {
        // Mirror libIPN WidgetExtensionsKt.setTopRight without a compile dep.
        Class<?> companionClass = Class.forName(
            "org.anti_ad.mc.common.gui.layout.AnchorStyles$Companion");
        Class<?> stylesClass = Class.forName("org.anti_ad.mc.common.gui.layout.AnchorStyles");
        Object companion = stylesClass.getField("Companion").get(null);
        Object topRight = companionClass.getMethod("getTopRight").invoke(companion);
        Method setAnchor = councilcraft$find(widget.getClass(), "setAnchor", stylesClass);
        setAnchor.invoke(widget, topRight);
        councilcraft$find(widget.getClass(), "setTop", int.class).invoke(widget, top);
        councilcraft$find(widget.getClass(), "setRight", int.class).invoke(widget, right);
    }

    private static Method councilcraft$find(Class<?> start, String name, Class<?>... params)
        throws NoSuchMethodException {
        Class<?> c = start;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }
}
