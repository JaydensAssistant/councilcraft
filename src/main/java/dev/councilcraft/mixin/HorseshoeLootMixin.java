package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.councilcraft.rules.GoldHorseshoeLoot;
import net.hyper_pigeon.horseshoes.Horseshoes;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PPM18. Horseshoes 1.0.3 injects gold horseshoes in code (its loot callback {@code lambda$onInitialize$5}),
 * not in data. Every injection goes through {@code LootTable.Builder.pool}. For the gold-only fortress and
 * bastion pools, the pool becomes one roll at the chance in {@link GoldHorseshoeLoot}; every other pool
 * passes through unchanged.
 *
 * <p>Required: if Horseshoes renames that lambda, mixin application fails and the log names this class.
 */
@Mixin(Horseshoes.class)
public abstract class HorseshoeLootMixin {
    @WrapOperation(
        method = "lambda$onInitialize$5",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/loot/LootTable$Builder;pool(Lnet/minecraft/loot/LootPool$Builder;)Lnet/minecraft/loot/LootTable$Builder;",
            remap = true
        )
    )
    private static LootTable.Builder councilcraft$rarerGold(LootTable.Builder table, LootPool.Builder pool,
                                                           Operation<LootTable.Builder> original,
                                                           @Local(argsOnly = true) Identifier id) {
        float chance = GoldHorseshoeLoot.chance(id.toString());
        if (!Float.isNaN(chance)) {
            pool.rolls(ConstantLootNumberProvider.create(1.0f)).conditionally(RandomChanceLootCondition.builder(chance));
        }
        return original.call(table, pool);
    }
}
