package com.tterrag.registrate.fabric;

import java.util.function.Supplier;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;

/**
 * Compile-time stand-in for Registrate Fabric 1.3.79's class of the same name (nested in Create's jar). Create's
 * VirtualFluid extends it; councilcraft's Process Ink is a VirtualFluid. Only the members councilcraft uses.
 * Not packed into our jar.
 */
public abstract class SimpleFlowableFluid extends FlowableFluid {
    protected SimpleFlowableFluid(Properties properties) {
    }

    public static class Properties {
        public Properties(Supplier<? extends Fluid> still, Supplier<? extends Fluid> flowing) {
        }
    }
}
