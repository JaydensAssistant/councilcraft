package dev.councilcraft.compat;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/**
 * PT43. The NBT half of the Supplementaries flag to vanilla banner swap that lets Small Ships 2.0.1 accept flags.
 * A flag put on a ship becomes the vanilla banner of the same colour carrying the flag's own NBT (its
 * {@code BlockEntityTag.Patterns} are the same shape as a banner's) plus a hidden marker holding the flag's item
 * id. Taking the marker off again gives back exactly the flag's NBT. Pure NBT, so a unit test can prove the
 * round trip without a game.
 */
public final class FlagBannerNbt {
    /** Hidden top-level key on the banner stack; its value is the original item id (the flag's wood and colour). */
    public static final String MARKER = "councilcraft_flag";

    private FlagBannerNbt() {
    }

    /** A copy of the flag's NBT (or a new compound when the flag has none) plus the marker. */
    public static NbtCompound mark(NbtCompound flagNbt, String flagItemId) {
        NbtCompound out = flagNbt == null ? new NbtCompound() : flagNbt.copy();
        out.putString(MARKER, flagItemId);
        return out;
    }

    public static boolean isMarked(NbtCompound nbt) {
        return nbt != null && nbt.contains(MARKER, NbtElement.STRING_TYPE);
    }

    /** The original item id, or null when the NBT carries no marker. */
    public static String originalId(NbtCompound nbt) {
        return isMarked(nbt) ? nbt.getString(MARKER) : null;
    }

    /** The NBT without the marker: null when nothing else is left, the way a plain flag has no NBT at all. */
    public static NbtCompound unmark(NbtCompound nbt) {
        if (nbt == null) {
            return null;
        }
        NbtCompound out = nbt.copy();
        out.remove(MARKER);
        return out.isEmpty() ? null : out;
    }
}
