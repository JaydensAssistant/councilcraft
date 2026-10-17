package dev.councilcraft.mixin;

import dev.councilcraft.rules.GoldHorseshoeLoot;
import java.util.List;
import net.hyper_pigeon.horseshoes.Horseshoes;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * S245. Horseshoes 1.0.3 registers one loot callback, {@code lambda$onInitialize$5},
 * and every gold horseshoe injection goes through {@code LootTable.Builder.pool}.
 * Redirecting that call strips gold entries. A pool that was only gold (nether
 * bridge, bastion bridge / hoglin stable / other / treasure) is not added.
 * Mixed pools (desert pyramid, end city) keep their iron and diamond entries.
 *
 * <p>If Horseshoes renames or removes that lambda, mixin application fails and
 * the game log names this class. That is deliberate: a silent miss would put
 * gold horseshoes back into chests.
 */
@Mixin(Horseshoes.class)
public abstract class GoldHorseshoeLootMixin {
    @Redirect(
        method = "lambda$onInitialize$5",
        remap = false,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/loot/LootTable$Builder;pool(Lnet/minecraft/loot/LootPool$Builder;)Lnet/minecraft/loot/LootTable$Builder;",
            remap = true
        )
    )
    private static LootTable.Builder councilcraft$stripGold(LootTable.Builder table, LootPool.Builder pool) {
        List<LootPoolEntry> entries = ((LootPoolBuilderAccess) pool).councilcraft$entries();
        entries.removeIf(GoldHorseshoeLootMixin::isGoldHorseshoe);
        if (entries.isEmpty()) {
            return table;
        }
        return table.pool(pool);
    }

    private static boolean isGoldHorseshoe(LootPoolEntry entry) {
        if (!(entry instanceof ItemEntry)) {
            return false;
        }
        Item item = ((ItemEntryAccess) entry).councilcraft$item();
        if (item == Horseshoes.GOLD_HORSESHOES_ITEM) {
            return true;
        }
        return GoldHorseshoeLoot.isGoldHorseshoe(Registries.ITEM.getId(item).toString());
    }
}
