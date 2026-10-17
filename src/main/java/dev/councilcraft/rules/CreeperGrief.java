package dev.councilcraft.rules;

/** S224. Mutant creepers and creeper minions damage entities and break no blocks. */
public final class CreeperGrief {
    private CreeperGrief() {
    }

    /** @return true when this explosion must use {@code DestructionType.KEEP} */
    public static boolean suppressBlockDamage(boolean mutantCreeperOrMinion) {
        return mutantCreeperOrMinion;
    }
}
