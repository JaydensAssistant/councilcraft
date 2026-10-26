package plus.dragons.createenchantmentindustry.common.fluids.printer.behaviour;

/** Name only. The real record is inside Create: Enchantment Industry; not packed into the mod jar. */
public record PrintingBehaviourProvider(int priority, PrintingBehaviour.Provider provider) {
    public static final int DEFAULT_PRIORITY = 0;
    public static final int BUILTIN_PRIORITY = 1000;
}
