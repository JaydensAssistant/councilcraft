package dev.councilcraft.mixin;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.nameplate.NameplateMain;
import net.nameplate.access.MobEntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PPM42. Nameplate 1.1.4's NBT read defaults HasMobRpgLabel to true when the key is missing (mobs placed
 * from structure templates, such as village pen cows), which bypasses its excludedEntities list. This
 * runs after Nameplate's own mixin (priority 1500, TAIL) and turns the label off again for excluded mobs,
 * so the list always wins on load. Applied only when Nameplate is loaded.
 */
@Mixin(value = MobEntity.class, priority = 1500)
public abstract class NameplateExcludedNbtMixin {
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void councilcraft$excludedAfterNbt(NbtCompound nbt, CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (!(self instanceof MobEntityAccess access) || !access.showMobRpgLabel()) {
            return;
        }
        // Same id form Nameplate uses for excludedEntities ("minecraft:cow").
        String type = self.getType().getTranslationKey().replace("entity.", "").replace(".", ":");
        if (NameplateMain.CONFIG != null && NameplateMain.CONFIG.excludedEntities.contains(type)) {
            access.setShowMobRpgLabel(false);
        }
    }
}
