package dev.councilcraft.respawn;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client only. Jayden: EMI is not a compile dependency. This reads
 * {@code EmiConfig.effectLocation} by reflection and treats only the enum
 * name {@code TOP} as the centered bar above the inventory. Any other
 * location, or a missing field, leaves the vanilla tooltip path alone.
 */
@Environment(EnvType.CLIENT)
public final class RebornEmi {
    private RebornEmi() {
    }

    public static boolean drawsEffectsOnTop() {
        if (!FabricLoader.getInstance().isModLoaded("emi")) {
            return false;
        }
        try {
            Class<?> config = Class.forName("dev.emi.emi.config.EmiConfig");
            Object location = config.getField("effectLocation").get(null);
            return isTop(location);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean isTop(Object location) {
        return location instanceof Enum<?> value && "TOP".equals(value.name());
    }
}
