package dev.councilcraft.respawn;

import dev.councilcraft.CouncilCraftRules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import dev.councilcraft.rules.RebornJump;
import virtuoel.pehkui.api.ScaleTypes;

/**
 * S80. The Reborn effect. Harmful, so it sits with the other debuffs.
 * The icon and the duration are vanilla. The duration counts down, and
 * {@link Reborn} rewrites it at each window boundary. There are no particles.
 * Size, speed, strength and hunger ease on the recovery fraction. Max health
 * steps by one point every 45 credited minutes.
 */
public final class RebornStatusEffect extends StatusEffect {
    public static final StatusEffect EFFECT = new RebornStatusEffect();

    private RebornStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 0x6B7F5A);
    }

    public static void register() {
        Registry.register(
            Registries.STATUS_EFFECT,
            new Identifier(CouncilCraftRules.MOD_ID, "reborn"),
            EFFECT
        );
    }

    public void apply(ServerPlayerEntity player, RebornStats stats) {
        float before = player.getMaxHealth();
        // Flat change from the 20-point base. 12 points is -8. 13 points is -7.
        modifier(
            player,
            EntityAttributes.GENERIC_MAX_HEALTH,
            CouncilCraftRules.REBORN_HEALTH_UUID,
            "councilcraft_reborn_health",
            stats.hitPoints - CouncilCraftRules.VANILLA_HP,
            EntityAttributeModifier.Operation.ADDITION
        );
        // MULTIPLY_TOTAL so 85% means the speed you would have had, times 0.85.
        // The amount is the factor minus one, and it eases toward zero.
        modifier(
            player,
            EntityAttributes.GENERIC_MOVEMENT_SPEED,
            CouncilCraftRules.REBORN_SPEED_UUID,
            "councilcraft_reborn_speed",
            stats.speed - 1.0,
            EntityAttributeModifier.Operation.MULTIPLY_TOTAL
        );
        modifier(
            player,
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            CouncilCraftRules.REBORN_MELEE_UUID,
            "councilcraft_reborn_melee",
            stats.melee - 1.0,
            EntityAttributeModifier.Operation.MULTIPLY_TOTAL
        );
        float after = player.getMaxHealth();
        if (player.getHealth() > after) {
            player.setHealth(after);
        } else if (after > before) {
            player.setHealth(Math.min(after, player.getHealth() + (after - before)));
        }
        scale(player, stats.scale);
    }

    public void clear(ServerPlayerEntity player) {
        remove(player, EntityAttributes.GENERIC_MAX_HEALTH, CouncilCraftRules.REBORN_HEALTH_UUID);
        remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, CouncilCraftRules.REBORN_SPEED_UUID);
        remove(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, CouncilCraftRules.REBORN_MELEE_UUID);
        scale(player, 1f);
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onRemoved(entity, attributes, amplifier);
        // Milk, a totem and /effect clear all come through here. While the real
        // timer is still running, leave the penalties in place. The next tick
        // puts the icon back. A real ending has already cleared the timer.
        if (entity instanceof ServerPlayerEntity player && !Reborn.running(player)) {
            clear(player);
        }
    }

    private static void scale(ServerPlayerEntity player, float scale) {
        set(ScaleTypes.BASE.getScaleData(player), scale);
        // PPM67: Pehkui divides fall distance by the size (FALLING is a motion-divisor type), so a
        // 60% baby took the damage of a 1.67x longer fall. FALLING at the same scale cancels that
        // and falls hurt like an adult's. clear() sets every type back to 1.
        set(ScaleTypes.FALLING.getScaleData(player), scale);
        // PM24: size, speed, strength and HP stay nerfed; jump apex and step height stay vanilla.
        set(ScaleTypes.JUMP_HEIGHT.getScaleData(player), RebornJump.jumpScale(scale));
        set(ScaleTypes.STEP_HEIGHT.getScaleData(player), RebornJump.stepScale(scale));
    }

    private static void set(virtuoel.pehkui.api.ScaleData data, float scale) {
        data.setScaleTickDelay(0);
        data.setTargetScale(scale);
        data.setScale(scale);
        data.setPersistence(Math.abs(scale - 1f) > 0.0001f);
    }

    private static void modifier(
        ServerPlayerEntity player,
        EntityAttribute attribute,
        java.util.UUID id,
        String name,
        double amount,
        EntityAttributeModifier.Operation operation
    ) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addPersistentModifier(new EntityAttributeModifier(id, name, amount, operation));
        }
    }

    private static void remove(ServerPlayerEntity player, EntityAttribute attribute, java.util.UUID id) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
