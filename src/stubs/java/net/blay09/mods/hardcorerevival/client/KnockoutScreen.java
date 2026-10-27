package net.blay09.mods.hardcorerevival.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Compile-time stand-in for Hardcore Revival 20.1.19 (PT55). Not packed into our jar.
 * Real constructor is package-protected; runtime code uses reflection.
 */
public class KnockoutScreen extends Screen {
    public KnockoutScreen() {
        super(Text.empty());
        throw new UnsupportedOperationException("stub");
    }
}
