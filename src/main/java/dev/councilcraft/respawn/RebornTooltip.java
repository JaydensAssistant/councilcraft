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
 * <p>CC-128AA. Shared ours lines are the sentence and the live Now line.
 * When the effect card or EMI's single-effect bar already shows the timer,
 * {@link RebornText#hover} is just those two lines. Icon-only lists (vanilla
 * narrow drawTooltip / EMI multi-effect row) keep vanilla's name + duration
 * via {@link RebornText#hoverKeepingVanillaTimer}, then ours. Deduping there
 * stops the two hooks from doubling lines. Growth Spurt still keeps its lines
 * and gains a description (PPM67). EMI's single-effect bar has no tooltip, so
 * {@link #hoverSingle} draws the two ours lines itself.
 */
@Environment(EnvType.CLIENT)
public final class RebornTooltip {
    private RebornTooltip() {
    }

    public static List<Text> linesFor(List<Text> existing) {
        if (existing == null || existing.isEmpty()) {
            return existing;
        }
        if (RebornText.isRebornName(existing.get(0))) {
            // Icon-only path: keep vanilla name + h:mm:ss, then sentence + Now.
            return RebornText.hoverKeepingVanillaTimer(existing, RebornClient.current());
        }
        if (RebornText.isGrowthSpurtName(existing.get(0))) {
            // PPM67. Growth Spurt keeps its name and time and gains one description line.
            return RebornText.withGrowthSpurtDescription(existing);
        }
        return existing;
    }

    /**
     * EMI draws one wide bar when the player has a single effect, and that bar
     * has no tooltip. The position matches EMI 1.1.24's centered layout.
     */
    public static void hoverSingle(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
        // A top-bar hover while EMI draws effects on the right would be a false tooltip.
        if (!RebornEmi.drawsEffectsOnTop()) {
            return;
        }
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
