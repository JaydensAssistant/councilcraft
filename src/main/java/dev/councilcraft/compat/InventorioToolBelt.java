package dev.councilcraft.compat;

import dev.councilcraft.rules.ToolBeltMigration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PPM68. One-time move of a saved Inventorio tool belt to the 4-slot layout (sword slot removed),
 * done on the server while the player's data loads. The old belt sword goes into the main inventory;
 * if that is full it is dropped at the player once they have joined (their world is set by then).
 */
public final class InventorioToolBelt {
    private static final Logger LOGGER = LoggerFactory.getLogger("councilcraft");
    public static final String SLOT_SWORD = "sword";
    private static final Map<UUID, List<ItemStack>> PENDING_DROPS = new ConcurrentHashMap<>();

    private InventorioToolBelt() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            List<ItemStack> stacks = PENDING_DROPS.remove(handler.player.getUuid());
            if (stacks != null) {
                stacks.forEach(handler.player.getInventory()::offerOrDrop);
            }
        });
    }

    /**
     * Rewrites the "ToolBelt" list of an unmigrated Inventorio compound in place and returns the
     * old sword-slot stacks, which no longer have a belt slot. Already migrated: nothing, empty list.
     */
    public static List<ItemStack> migrate(NbtCompound inventorioTag) {
        List<ItemStack> extracted = new ArrayList<>();
        if (!ToolBeltMigration.needsMigration(inventorioTag.getInt(ToolBeltMigration.MARKER))) {
            return extracted;
        }
        NbtList belt = inventorioTag.getList("ToolBelt", NbtElement.COMPOUND_TYPE);
        NbtList moved = new NbtList();
        for (int i = 0; i < belt.size(); i++) {
            NbtCompound item = belt.getCompound(i).copy();
            int slot = ToolBeltMigration.remap(item.getInt("Slot"));
            if (slot == ToolBeltMigration.EXTRACT) {
                ItemStack stack = ItemStack.fromNbt(item);
                if (!stack.isEmpty()) {
                    extracted.add(stack);
                }
                continue;
            }
            item.putInt("Slot", slot);
            moved.add(item);
        }
        inventorioTag.put("ToolBelt", moved);
        return extracted;
    }

    /** Puts the old belt swords in the main inventory; what does not fit is dropped after join. */
    public static void giveBack(PlayerEntity player, List<ItemStack> swords) {
        if (swords.isEmpty()) {
            return;
        }
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack stack : swords) {
            player.getInventory().insertStack(stack);
            if (!stack.isEmpty()) {
                left.add(stack);
            }
        }
        LOGGER.info("CouncilCraft: moved {} tool-belt sword stack(s) of {} to the main inventory ({} to drop).",
            swords.size(), player.getName().getString(), left.size());
        if (!left.isEmpty()) {
            PENDING_DROPS.computeIfAbsent(player.getUuid(), id -> new ArrayList<>()).addAll(left);
        }
    }
}
