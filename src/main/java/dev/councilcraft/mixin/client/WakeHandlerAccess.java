package dev.councilcraft.mixin.client;

import com.goby56.wakes.simulation.WakeHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** PPM80 (B). Wakes' private {@code WakeHandler.reset()}, which clears every stored wake. Applied only with Wakes. */
@Mixin(value = WakeHandler.class, remap = false)
public interface WakeHandlerAccess {
    @Invoker("reset")
    void councilcraft$reset();
}
