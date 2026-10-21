package dev.ithundxr.createnumismatics.content.backend.behaviours;

import java.util.List;
import net.minecraft.text.MutableText;

/**
 * Compile-time stand-in for Numismatics 1.1.0's class of the same name, which implements a Create interface
 * the compiler cannot see. Only the type and the method named by the PPM8 price mixins are declared.
 * Not packed into our jar.
 */
public class SliderStylePriceBehaviour {
    public List<MutableText> getCondensedPriceBreakdown() {
        throw new UnsupportedOperationException("stub");
    }
}
