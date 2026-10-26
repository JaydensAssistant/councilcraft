package net.irisshaders.iris.gl.uniform;

import java.util.function.DoubleSupplier;

/**
 * Name only. The real interface is in Iris 1.7.6+mc1.20.1 (built with a newer Loom than ours).
 * Compiled for the PT13 portal fade uniform and not packed into the mod jar.
 */
public interface UniformHolder {
    UniformHolder uniform1f(UniformUpdateFrequency updateFrequency, String name, DoubleSupplier value);
}
