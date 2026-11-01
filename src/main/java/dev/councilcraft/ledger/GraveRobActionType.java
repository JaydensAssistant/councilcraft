package dev.councilcraft.ledger;

import com.github.quiltservertools.ledger.actions.AbstractActionType;
import java.util.List;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * CC-155. Ledger action type id {@code grave-rob} (≤16 chars). Soft-depend:
 * only registered when Ledger is present. Subclasses {@link AbstractActionType}.
 */
public final class GraveRobActionType extends AbstractActionType {
    @NotNull
    @Override
    public String getIdentifier() {
        return GraveRobLedger.ACTION_ID;
    }

    /**
     * Build one action summarizing the loot. Extra data = owner name;
     * source = looter; object = first item id; objectState = total item count.
     */
    public static GraveRobActionType build(ServerPlayerEntity actor, UUID ownerId, String ownerName,
            BlockPos pos, World world, List<ItemStack> items) {
        GraveRobActionType action = new GraveRobActionType();
        action.setPos(pos);
        action.setWorld(world.getRegistryKey().getValue());
        action.setSourceProfile(actor.getGameProfile());
        action.setSourceName(actor.getGameProfile().getName());
        String owner = ownerName != null && !ownerName.isEmpty()
            ? ownerName
            : (ownerId != null ? ownerId.toString() : "?");
        action.setExtraData(owner);
        ItemStack first = null;
        int total = 0;
        if (items != null) {
            for (ItemStack stack : items) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                total += stack.getCount();
                if (first == null) {
                    first = stack;
                }
            }
        }
        if (first != null) {
            action.setObjectResourceLocation(Registries.ITEM.getId(first.getItem()));
            action.setObjectState(String.valueOf(total));
        } else {
            action.setObjectResourceLocation(new Identifier("minecraft", "air"));
            action.setObjectState("0");
        }
        return action;
    }
}
