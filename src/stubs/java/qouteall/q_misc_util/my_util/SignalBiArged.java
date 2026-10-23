package qouteall.q_misc_util.my_util;

import java.util.function.BiConsumer;

/**
 * Compile-time stand-in for Immersive Portals 5.2.0's class of the same name (q_misc_util jar nested inside
 * Immersive Portals, which the compiler cannot see). PPM100. Not packed into our jar.
 */
public class SignalBiArged<A, B> {
    public void connect(BiConsumer<A, B> func) {
        throw new UnsupportedOperationException("stub");
    }
}
