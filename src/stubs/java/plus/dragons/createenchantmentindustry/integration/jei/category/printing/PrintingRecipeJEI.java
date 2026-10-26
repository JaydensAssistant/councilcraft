package plus.dragons.createenchantmentindustry.integration.jei.category.printing;

import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.util.Identifier;

/** Name only. The real interface is inside Create: Enchantment Industry's JEI integration; not packed. */
public interface PrintingRecipeJEI {
    void setBase(IRecipeSlotBuilder slot);

    void setTemplate(IRecipeSlotBuilder slot);

    void setFluid(IRecipeSlotBuilder slot);

    void setOutput(IRecipeSlotBuilder slot);

    Type getType();

    default Identifier getRegistryName() {
        return getType().id();
    }

    record Type(Identifier id) {
    }
}
