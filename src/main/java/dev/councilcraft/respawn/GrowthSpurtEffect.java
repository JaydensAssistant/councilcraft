package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * PM24a. Growth Spurt: given to a Reborn player who is fed by another player. While it lasts, credited
 * Reborn windows count three times (Reborn.tick). AFK windows still count nothing. No attributes.
 */
public final class GrowthSpurtEffect extends StatusEffect {
    public static final StatusEffect EFFECT = new GrowthSpurtEffect();

    private GrowthSpurtEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x8BC34A);
    }

    public static void register() {
        Registry.register(Registries.STATUS_EFFECT, new Identifier(CouncilCraftRules.MOD_ID, "growth_spurt"), EFFECT);
    }
}
