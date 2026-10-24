package dev.councilcraft.client.compat;

import dev.councilcraft.rules.ControllerRadialDefaults;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.bindings.BindContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PPM105. Controller action "Next quiver arrow". Controlify's radial menu only taps a key for about two
 * ticks, and Supplementaries' quiver key needs holding plus scrolling, so this action sends
 * Supplementaries' own cycle message instead (see {@link SupplementariesQuiverCycle}). One press, or
 * one radial pick (radial slot 7 by default, {@link ControllerRadialDefaults#EXTRAS}), selects the next
 * arrow. Runs from the {@code controlify} entrypoint, which Controlify calls before it locks its
 * binding registry; this class uses only Controlify and Minecraft types.
 */
public final class ControlifyQuiverCycle implements ControlifyEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("councilcraft");
    private static boolean failed;

    /** Both mods are needed: Controlify for the action, Supplementaries for the quiver. */
    public static boolean shouldRegister() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("controlify") && loader.isModLoaded("supplementaries");
    }

    @Override
    public void onControlifyInit(ControlifyApi controlify) {
        if (!shouldRegister()) {
            return;
        }
        try {
            register();
        } catch (LinkageError | RuntimeException e) {
            LOGGER.error("[councilcraft] Could not register the Next quiver arrow controller action", e);
        }
    }

    @Override
    public void onControllersDiscovered(ControlifyApi controlify) {
    }

    private static void register() {
        Identifier id = new Identifier(ControllerRadialDefaults.NEXT_QUIVER_ARROW);
        ControlifyBindApi api = ControlifyBindApi.get();
        ItemStack quiver = new ItemStack(Registries.ITEM.get(new Identifier("supplementaries", "quiver")));
        ItemStack icon = quiver.isEmpty() ? new ItemStack(Items.ARROW) : quiver;
        api.registerRadialIcon(id, (graphics, x, y, tickDelta) -> graphics.drawItem(icon, x, y));
        InputBindingSupplier binding = api.registerBinding(builder -> builder
            .id(id)
            .name(Text.translatable("councilcraft.controller.next_quiver_arrow"))
            .description(Text.translatable("councilcraft.controller.next_quiver_arrow.desc"))
            .category(Text.translatable("key.categories.councilcraft"))
            .allowedContexts(BindContext.IN_GAME)
            .radialCandidate(id));
        ControlifyEvents.ACTIVE_CONTROLLER_TICKED.register(event -> {
            InputBinding input = binding.onOrNull(event.controller());
            if (input != null && input.justPressed()) {
                nextArrow();
            }
        });
    }

    private static void nextArrow() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (failed || client.player == null) {
            return;
        }
        try {
            SupplementariesQuiverCycle.next(client.player);
        } catch (LinkageError | RuntimeException e) {
            // Supplementaries' message is not a stable API; stop trying after the first failure.
            failed = true;
            LOGGER.error("[councilcraft] Next quiver arrow failed; Supplementaries' quiver message may have changed", e);
        }
    }
}
