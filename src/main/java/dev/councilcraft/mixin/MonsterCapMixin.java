package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.councilcraft.rules.MobBand;
import net.minecraft.entity.SpawnGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** PPM81 (3). Monster mob cap 70 -> {@link MobBand#MONSTER_CAP}; the other categories keep vanilla caps. */
@Mixin(SpawnGroup.class)
public abstract class MonsterCapMixin {
    @ModifyReturnValue(method = "getCapacity", at = @At("RETURN"))
    private int councilcraft$monsterCap(int capacity) {
        return (Object) this == SpawnGroup.MONSTER ? MobBand.MONSTER_CAP : capacity;
    }
}
