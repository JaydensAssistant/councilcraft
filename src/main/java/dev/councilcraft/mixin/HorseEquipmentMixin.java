package dev.councilcraft.mixin;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.HorseJump;
import dev.councilcraft.rules.HorseStep;
import dev.councilcraft.rules.HorseshoeTiers;
import net.hyper_pigeon.horseshoes.items.HorseshoesItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * S223 / S230. Step height is a plain field. It is not an attribute and it is
 * not synced, so both sides set it every tick from the shoes in the feet slot.
 * Horseshoes copies the shoe stack into that slot on the server. This mixin
 * also clears the slot when the shoes come off, and adds netherite knockback
 * resistance, which the Horseshoes mod does not.
 *
 * <p>PPM15. Horses, donkeys and mules also get a transient jump-strength modifier from {@link HorseJump}:
 * the re-ranged jump plus the shoes' blocks. It is recomputed from the bred base value whenever that or
 * the shoes change, so it never compounds and existing horses rescale too. Camels stay vanilla.
 */
@Mixin(AbstractHorseEntity.class)
public abstract class HorseEquipmentMixin {
    @Shadow
    protected SimpleInventory items;

    @Unique
    private float councilcraft$vanillaStep = Float.NaN;
    @Unique
    private double councilcraft$jumpBase = Double.NaN;
    @Unique
    private float councilcraft$jumpShoes = Float.NaN;

    @Inject(method = "tick", at = @At("HEAD"))
    private void councilcraft$riddenStep(CallbackInfo ci) {
        AbstractHorseEntity horse = (AbstractHorseEntity) (Object) this;
        if (Float.isNaN(this.councilcraft$vanillaStep)) {
            this.councilcraft$vanillaStep = horse.getStepHeight();
        }
        ItemStack feet = horse.getEquippedStack(EquipmentSlot.FEET);
        float shoeStep = HorseshoeTiers.stepHeight(Registries.ITEM.getId(feet.getItem()).toString());
        horse.setStepHeight(HorseStep.height(horse.hasPassengers(), shoeStep, this.councilcraft$vanillaStep));
        if (!horse.getWorld().isClient) {
            councilcraft$jump(horse, HorseshoeTiers.jumpBlocks(Registries.ITEM.getId(feet.getItem()).toString()));
        }
    }

    @Unique
    private void councilcraft$jump(AbstractHorseEntity horse, float shoeBlocks) {
        if (!(horse instanceof HorseEntity || horse instanceof DonkeyEntity || horse instanceof MuleEntity)) {
            return;
        }
        EntityAttributeInstance jump = horse.getAttributeInstance(EntityAttributes.HORSE_JUMP_STRENGTH);
        if (jump == null) {
            return;
        }
        double base = jump.getBaseValue();
        if (base == this.councilcraft$jumpBase && shoeBlocks == this.councilcraft$jumpShoes
            && jump.getModifier(CouncilCraftRules.HORSE_JUMP_UUID) != null) {
            return;
        }
        this.councilcraft$jumpBase = base;
        this.councilcraft$jumpShoes = shoeBlocks;
        jump.removeModifier(CouncilCraftRules.HORSE_JUMP_UUID);
        jump.addTemporaryModifier(new EntityAttributeModifier(
            CouncilCraftRules.HORSE_JUMP_UUID,
            "CouncilCraft horse jump",
            HorseJump.strength(base, shoeBlocks) - base,
            EntityAttributeModifier.Operation.ADDITION
        ));
    }

    @Inject(method = "onInventoryChanged", at = @At("TAIL"))
    private void councilcraft$shoesChanged(Inventory inventory, CallbackInfo ci) {
        AbstractHorseEntity horse = (AbstractHorseEntity) (Object) this;
        if (horse.getWorld().isClient) {
            return;
        }
        int slot = horse.hasArmorSlot()
            ? CouncilCraftRules.HORSESHOE_SLOT_WITH_ARMOR
            : CouncilCraftRules.HORSESHOE_SLOT_WITHOUT_ARMOR;
        ItemStack shoes = this.items.getStack(slot);
        councilcraft$syncFeet(horse, shoes);
        councilcraft$knockback(horse, Registries.ITEM.getId(shoes.getItem()).toString());
    }

    @Unique
    private static void councilcraft$syncFeet(AbstractHorseEntity horse, ItemStack shoes) {
        ItemStack feet = horse.getEquippedStack(EquipmentSlot.FEET);
        if (shoes.getItem() instanceof HorseshoesItem) {
            horse.equipStack(EquipmentSlot.FEET, shoes);
            horse.setEquipmentDropChance(EquipmentSlot.FEET, 0.0f);
        } else if (feet.getItem() instanceof HorseshoesItem) {
            horse.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
        }
    }

    @Unique
    private static void councilcraft$knockback(AbstractHorseEntity horse, String shoeId) {
        EntityAttributeInstance knockback = horse.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockback == null) {
            return;
        }
        knockback.removeModifier(CouncilCraftRules.NETHERITE_HORSESHOE_KNOCKBACK_UUID);
        float amount = HorseshoeTiers.knockbackResistance(shoeId);
        if (amount <= 0.0f) {
            return;
        }
        knockback.addPersistentModifier(new EntityAttributeModifier(
            CouncilCraftRules.NETHERITE_HORSESHOE_KNOCKBACK_UUID,
            "Netherite horseshoes knockback resistance",
            amount,
            EntityAttributeModifier.Operation.ADDITION
        ));
    }
}
