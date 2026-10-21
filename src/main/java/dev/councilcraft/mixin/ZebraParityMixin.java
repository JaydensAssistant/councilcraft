package dev.councilcraft.mixin;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.compat.Zebras;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.HorseArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPPM3. Zebras wear horse armor like horses: an armor slot (inventory index 1, as for horses), the armor's
 * protection, and the armor stack in the chest equipment slot so clients see it ({@code ZebraArmorFeatureRenderer}).
 * Zebras extend the donkey class, which does not save index 1, so the armor is saved as "ArmorItem" like a horse's.
 * Horseshoes then sit at index 2 (Horseshoes' rule for entities with an armor slot) and the chest starts at 3.
 */
@Mixin(AbstractHorseEntity.class)
public abstract class ZebraParityMixin {
    @Shadow
    protected SimpleInventory items;

    @Inject(method = "hasArmorSlot", at = @At("HEAD"), cancellable = true)
    private void councilcraft$zebraArmorSlot(CallbackInfoReturnable<Boolean> cir) {
        if (Zebras.isZebra((AbstractHorseEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isHorseArmor", at = @At("HEAD"), cancellable = true)
    private void councilcraft$zebraArmor(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Zebras.isZebra((AbstractHorseEntity) (Object) this)) {
            cir.setReturnValue(stack.getItem() instanceof HorseArmorItem);
        }
    }

    @Inject(method = "updateSaddle", at = @At("TAIL"))
    private void councilcraft$zebraEquipArmor(CallbackInfo ci) {
        AbstractHorseEntity horse = (AbstractHorseEntity) (Object) this;
        if (horse.getWorld().isClient || !Zebras.isZebra(horse) || this.items.size() < 2) {
            return;
        }
        ItemStack armor = this.items.getStack(1);
        boolean isArmor = armor.getItem() instanceof HorseArmorItem;
        horse.equipStack(EquipmentSlot.CHEST, isArmor ? armor : ItemStack.EMPTY);
        horse.setEquipmentDropChance(EquipmentSlot.CHEST, 0.0f);
        EntityAttributeInstance attribute = horse.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(CouncilCraftRules.ZEBRA_ARMOR_UUID);
        int bonus = isArmor ? ((HorseArmorItem) armor.getItem()).getBonus() : 0;
        if (bonus != 0) {
            attribute.addTemporaryModifier(new EntityAttributeModifier(
                CouncilCraftRules.ZEBRA_ARMOR_UUID, "Zebra armor bonus", bonus, EntityAttributeModifier.Operation.ADDITION));
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void councilcraft$zebraWriteArmor(NbtCompound nbt, CallbackInfo ci) {
        if (Zebras.isZebra((AbstractHorseEntity) (Object) this) && this.items.size() > 1 && !this.items.getStack(1).isEmpty()) {
            nbt.put("ArmorItem", this.items.getStack(1).writeNbt(new NbtCompound()));
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void councilcraft$zebraReadArmor(NbtCompound nbt, CallbackInfo ci) {
        if (!Zebras.isZebra((AbstractHorseEntity) (Object) this) || !nbt.contains("ArmorItem", 10) || this.items.size() < 2) {
            return;
        }
        ItemStack armor = ItemStack.fromNbt(nbt.getCompound("ArmorItem"));
        if (armor.getItem() instanceof HorseArmorItem) {
            this.items.setStack(1, armor);
        }
        this.updateSaddle();
    }

    @Shadow
    protected abstract void updateSaddle();
}
