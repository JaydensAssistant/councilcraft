package dev.councilcraft.mixin.client;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Client only. Reads the inventory position without shadowing fields on a
 * child screen. Shadowing those inherited fields crashed the client.
 */
@Mixin(HandledScreen.class)
public interface HandledScreenAccess {
    @Accessor("x")
    int councilcraft$x();

    @Accessor("y")
    int councilcraft$y();

    @Accessor("backgroundWidth")
    int councilcraft$backgroundWidth();

    /** PPM88. The slot under the mouse, or null. */
    @Accessor("focusedSlot")
    Slot councilcraft$focusedSlot();
}
