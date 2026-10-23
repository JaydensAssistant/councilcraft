package dev.councilcraft.client.input;

import de.rubixdev.inventorio.client.ui.InventorioScreen;
import de.rubixdev.inventorio.player.InventorioScreenHandler;
import dev.councilcraft.CouncilCraft;
import dev.councilcraft.client.compat.ControlifyHints;
import dev.councilcraft.mixin.client.HandledScreenAccess;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import kotlin.ranges.IntRange;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * PPM88. Makes the utility belt discoverable in Inventorio's screen.
 * <ul>
 *   <li>(a) The first time the player puts an item into an empty belt slot, a one-time toast names the switch keys:
 *   the player's bound {@link UtilityBeltScroll} key plus scroll, or Controlify's prev/next utility glyphs on a
 *   controller. Seen once per player UUID, stored in {@code config/councilcraft-hints.properties}.</li>
 *   <li>(b) Hovering an empty, enabled belt slot shows the same tip as a tooltip.</li>
 * </ul>
 * Client only. Registered only with Inventorio; the Controlify parts load only with Controlify.
 */
public final class UtilityBeltHints {
    private static final String FILE = "councilcraft-hints.properties";
    private static final String TOAST_TITLE = "councilcraft.hint.utility_belt.title";
    private static final String KEYBOARD_TIP = "councilcraft.hint.utility_belt.keyboard";
    private static final String CONTROLLER_TIP = "councilcraft.hint.utility_belt.controller";

    private UtilityBeltHints() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof InventorioScreen inventorio)) {
                return;
            }
            InventorioScreenHandler handler = inventorio.getScreenHandler();
            if (client.player != null && !seen(client.player.getUuid())) {
                boolean[] wasEmpty = beltEmptiness(handler);
                ScreenEvents.afterTick(screen).register(s -> {
                    if (wasEmpty[0] && filledEmptySlot(handler, wasEmpty)) {
                        showToast(client);
                        wasEmpty[0] = false; // stop checking for this screen
                    }
                });
            }
            ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, delta) -> {
                Slot slot = ((HandledScreenAccess) s).councilcraft$focusedSlot();
                if (slot != null && inBelt(slot.id) && slot.isEnabled() && !slot.hasStack()
                    && handler.getCursorStack().isEmpty()) {
                    context.drawTooltip(client.textRenderer, List.of(tip()), mouseX, mouseY);
                }
            });
        });
    }

    /** The tip for the current input: the bound modifier key, or the controller glyphs. */
    public static Text tip() {
        if (FabricLoader.getInstance().isModLoaded("controlify") && ControlifyHints.onController()) {
            return Text.translatable(CONTROLLER_TIP, ControlifyHints.prevUtilityGlyph(), ControlifyHints.nextUtilityGlyph());
        }
        KeyBinding modifier = UtilityBeltScroll.modifier();
        Text key = modifier != null ? modifier.getBoundKeyLocalizedText() : Text.translatable(UtilityBeltScroll.KEY);
        return Text.translatable(KEYBOARD_TIP, key);
    }

    private static boolean inBelt(int slotId) {
        IntRange belt = InventorioScreenHandler.utilityBeltRange;
        return slotId >= belt.getFirst() && slotId <= belt.getLast();
    }

    /**
     * Index 0 is "still watching"; index 1 + i is whether belt slot i was empty. Returns the array with
     * index 0 true.
     */
    private static boolean[] beltEmptiness(InventorioScreenHandler handler) {
        IntRange belt = InventorioScreenHandler.utilityBeltRange;
        boolean[] empty = new boolean[belt.getLast() - belt.getFirst() + 2];
        empty[0] = true;
        for (int id = belt.getFirst(); id <= belt.getLast() && id < handler.slots.size(); id++) {
            empty[1 + id - belt.getFirst()] = !handler.slots.get(id).hasStack();
        }
        return empty;
    }

    /** True when a belt slot that was empty now holds an item. Updates the snapshot. */
    private static boolean filledEmptySlot(InventorioScreenHandler handler, boolean[] wasEmpty) {
        IntRange belt = InventorioScreenHandler.utilityBeltRange;
        boolean filled = false;
        for (int id = belt.getFirst(); id <= belt.getLast() && id < handler.slots.size(); id++) {
            boolean empty = !handler.slots.get(id).hasStack();
            int i = 1 + id - belt.getFirst();
            if (wasEmpty[i] && !empty) {
                filled = true;
            }
            wasEmpty[i] = empty;
        }
        return filled;
    }

    private static void showToast(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        client.getToastManager().add(SystemToast.create(client, SystemToast.Type.TUTORIAL_HINT,
            Text.translatable(TOAST_TITLE), tip()));
        Properties stored = load();
        stored.setProperty(seenKey(client.player.getUuid()), "true");
        save(stored);
    }

    static String seenKey(UUID player) {
        return "utility_belt_toast_seen." + player;
    }

    private static boolean seen(UUID player) {
        return Boolean.parseBoolean(load().getProperty(seenKey(player)));
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    private static Properties load() {
        Properties properties = new Properties();
        Path path = file();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                CouncilCraft.LOGGER.warn("CouncilCraft hints: could not read {}", path, e);
            }
        }
        return properties;
    }

    private static void save(Properties properties) {
        try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            properties.store(writer, "CouncilCraft one-time hints (PPM88), per player UUID.");
        } catch (IOException e) {
            CouncilCraft.LOGGER.error("CouncilCraft hints: could not save {}", file(), e);
        }
    }
}
