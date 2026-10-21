package plus.dragons.createenchantmentindustry.common.processing.forger;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Name only. The real class is inside Create: Enchantment Industry 2.5.2.
 * This file is compiled for the mixin signatures and is not packed into the mod jar.
 */
public class BlazeForgerInventory {
    /** The real type is a record with these accessors. */
    public static final class Result {
        public Status status() {
            throw new AssertionError();
        }

        public ItemStack primaryOutput() {
            throw new AssertionError();
        }

        public ItemStack secondaryOutput() {
            throw new AssertionError();
        }

        public BlazeForgerMode operation() {
            throw new AssertionError();
        }

        public static Result invalid(BlazeForgerMode mode, Text failure) {
            throw new AssertionError();
        }
    }

    public enum Status {
        EMPTY_INPUT,
        INCOMPLETE_INPUT,
        INVALID,
        READY
    }
}
