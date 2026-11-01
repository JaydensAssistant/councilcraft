package dev.councilcraft.ledger;

import com.github.quiltservertools.ledger.Ledger;
import com.github.quiltservertools.ledger.registry.ActionRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CC-155 / CC-156. Soft-depend Ledger registration and logging for grave-rob.
 * Compile-only against Ledger; OPTIONAL_MIXINS keep Forgotten Graves optional.
 */
public final class GraveRobLedger {
    public static final String ACTION_ID = "grave-rob";
    private static final Logger LOGGER = LoggerFactory.getLogger("councilcraft/grave-rob");
    private static boolean registered;
    private static boolean unavailable;

    private GraveRobLedger() {
    }

    public static void register() {
        if (registered || unavailable) {
            return;
        }
        try {
            ActionRegistry.INSTANCE.registerActionType(GraveRobActionType::new);
            registered = true;
            LOGGER.info("Registered Ledger action type {}", ACTION_ID);
        } catch (NoClassDefFoundError e) {
            unavailable = true;
            LOGGER.info("Ledger not present; grave-rob logging disabled");
        } catch (Throwable t) {
            unavailable = true;
            LOGGER.warn("Failed to register Ledger grave-rob action: {}", t.toString());
        }
    }

    public static boolean isRegistered() {
        return registered;
    }

    public static void logIfRob(ServerPlayerEntity actor, UUID ownerId, String ownerName,
            BlockPos pos, World world, List<ItemStack> items) {
        if (!registered || actor == null || world == null || pos == null) {
            return;
        }
        if (!GraveRobDecision.shouldLog(actor.getUuid(), ownerId)) {
            return;
        }
        try {
            List<ItemStack> copy = items == null ? List.of() : new ArrayList<>(items);
            Ledger.getApi().logAction(GraveRobActionType.build(actor, ownerId, ownerName, pos, world, copy));
        } catch (Throwable t) {
            LOGGER.warn("grave-rob log failed: {}", t.toString());
        }
    }
}
