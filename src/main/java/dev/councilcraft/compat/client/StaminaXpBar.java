package dev.councilcraft.compat.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import tictim.paraglider.ParagliderMod;
import tictim.paraglider.api.movement.Movement;
import tictim.paraglider.api.movement.ParagliderPlayerStates;
import tictim.paraglider.api.stamina.Stamina;
import tictim.paraglider.client.screen.DisableStaminaRender;

/**
 * PM37. Paragliders' stamina drawn in the XP bar slot with vanilla's horse jump-bar textures
 * (icons.png: 84 = empty bar, 89 = charge, 74 = the greyed cooldown bar, used here while depleted).
 * Shown while gliding or while stamina is below max. The XP bar and level return once it is full.
 * Only loaded when Paragliders is present.
 */
public final class StaminaXpBar {
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    private static final int WIDTH = 182;

    private StaminaXpBar() {
    }

    /** Same gates Paragliders uses for its wheel, plus "not full, or gliding". */
    public static boolean shouldShow(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.currentScreen instanceof DisableStaminaRender) {
            return false;
        }
        Stamina stamina = Stamina.get(player);
        if (!stamina.renderStaminaWheel() || !ParagliderMod.instance().getPlayerStateMap().hasStaminaConsumption()) {
            return false;
        }
        return stamina.stamina() < stamina.maxStamina()
            || Movement.get(player).state().flags().contains(ParagliderPlayerStates.Flags.FLAG_PARAGLIDING);
    }

    public static void draw(MinecraftClient client, DrawContext context, int x, int scaledHeight) {
        Stamina stamina = Stamina.get(client.player);
        int max = Math.max(1, stamina.maxStamina());
        int filled = fillWidth(stamina.stamina(), max);
        int y = scaledHeight - 32 + 3;
        context.drawTexture(ICONS, x, y, 0, 84, WIDTH, 5);
        if (filled > 0) {
            context.drawTexture(ICONS, x, y, 0, stamina.isDepleted() ? 74 : 89, filled, 5);
        }
    }

    /** Vanilla's bar maths: (int) (fraction * 183), never wider than the 182-pixel texture. */
    public static int fillWidth(int stamina, int max) {
        float fraction = Math.max(0, Math.min(stamina, max)) / (float) max;
        return Math.min(WIDTH, (int) (fraction * 183.0f));
    }
}
