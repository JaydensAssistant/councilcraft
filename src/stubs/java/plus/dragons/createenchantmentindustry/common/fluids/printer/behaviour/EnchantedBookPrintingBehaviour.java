package plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour;

import com.mojang.serialization.DataResult;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Name only. The real class is inside Create: Enchantment Industry; not packed into the mod jar. */
public final class EnchantedBookPrintingBehaviour {
    private int cost;

    public static Optional<DataResult<PrintingBehaviour>> create(World world, SmartFluidTankBehaviour tank, ItemStack template) {
        throw new UnsupportedOperationException("stub");
    }
}
