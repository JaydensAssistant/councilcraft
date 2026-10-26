package plus.dragons.createenchantmentindustry.config;

import net.createmod.catnip.config.ConfigBase;

/** Name only. The real class is inside Create: Enchantment Industry; not packed into the mod jar. */
public class CEIFluidsConfig extends ConfigBase {
    public final ConfigBase.ConfigInt printerFluidCapacity = null;
    public final ConfigBase.ConfigBool enableEnchantedBookPrinting = null;

    @Override
    public String getName() {
        return "fluids";
    }
}
