package dev.councilcraft.respawn;

import dev.councilcraft.mixin.client.HandledScreenAccess;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;

/**
 * Client only. Jayden: this is one of the client tooltip files.
 *
 * <p>The hover is two lines, the sentence and the live "Now" line.
 * An existing effect tooltip whose first line is the Reborn name is replaced
 * with those two lines, so a second tooltip is not drawn on top.
 * EMI's single-effect bar draws the name and the time in the bar and opens
 * no tooltip, so that one case draws the two lines itself.
 */
@Environment(EnvType.CLIENT)
public final class RebornTooltip {
    private RebornTooltip() {
    }

    public static List<Text> linesFor(List<Text> existing) {
        if (existing == null || existing.isEmpty() || !RebornText.isRebornName(existing.get(0))) {
            return existing;
        }
        return RebornText.hover(RebornClient.current());
    }

    /**
     * EMI draws one wide bar when the player has a single effect, and that bar
     * has no tooltip. The position matches EMI 1.1.24's centered layout.
     */
    public static void hoverSingle(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.getStatusEffects().size() != 1) {
            return;
        }
        StatusEffectInstance only = client.player.getStatusEffects().iterator().next();
        if (only.getEffectType() != RebornStatusEffect.EFFECT || !overSingleBar(screen, mouseX, mouseY)) {
            return;
        }
        context.drawTooltip(
            client.textRenderer,
            RebornText.hover(RebornClient.current()),
            Optional.empty(),
            mouseX,
            Math.max(mouseY, 16)
        );
    }

    private static boolean overSingleBar(HandledScreen<?> screen, int mouseX, int mouseY) {
        HandledScreenAccess access = (HandledScreenAccess) screen;
        int y = access.councilcraft$y() - 34;
        if (screen instanceof CreativeInventoryScreen || FabricLoader.getInstance().isModLoaded("inventorytabs")) {
            y -= 28;
        }
        int width = 120;
        int x = access.councilcraft$x() + (access.councilcraft$backgroundWidth() - width) / 2;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 32;
    }
}
