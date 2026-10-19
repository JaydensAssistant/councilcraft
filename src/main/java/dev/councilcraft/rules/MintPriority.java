package dev.councilcraft.rules;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import dev.councilcraft.CouncilCraftRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;

/**
 * Create's press treats the vanilla 3x3 diamond-block craft as a basin recipe.
 * Extra items do not stop it matching, and a recipe it has already chosen keeps
 * running after the mold arrives. Sprocket and cog are the mint recipes that
 * lose that race. While either mold is in the basin, a diamond-block result is
 * not a match, and a press cycle that already chose it is dropped before Create
 * consumes the diamonds.
 */
public final class MintPriority {
    private MintPriority() {
    }

    public static boolean competesWithDiamondBlock(String itemId) {
        return CouncilCraftRules.SPROCKET_MOLD_ID.equals(itemId) || CouncilCraftRules.COG_MOLD_ID.equals(itemId);
    }

    /** True when this result must not run because a competing mold is in the basin. */
    public static boolean suppressDiamondBlock(boolean competingMoldPresent, String resultId) {
        return competingMoldPresent && CouncilCraftRules.DIAMOND_BLOCK_ID.equals(resultId);
    }

    public static boolean competingMold(Iterable<String> itemIds) {
        for (String itemId : itemIds) {
            if (competesWithDiamondBlock(itemId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean basinSuppressesDiamondBlock(BasinBlockEntity basin, Recipe<?> recipe) {
        if (basin == null || recipe == null || basin.getWorld() == null) {
            return false;
        }
        World world = basin.getWorld();
        return suppressDiamondBlock(
            competingMold(itemIds(basin.getInputInventory())),
            Registries.ITEM.getId(recipe.getOutput(world.getRegistryManager()).getItem()).toString()
        );
    }

    private static List<String> itemIds(Inventory inventory) {
        List<String> ids = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ids.add(Registries.ITEM.getId(stack.getItem()).toString());
        }
        return ids;
    }
}
