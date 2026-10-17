package dev.councilcraft.mixin;

import java.util.List;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.LootPoolEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the entries Horseshoes has already added to a pool builder, before the pool is built. */
@Mixin(LootPool.Builder.class)
public interface LootPoolBuilderAccess {
    @Accessor("entries")
    List<LootPoolEntry> councilcraft$entries();
}
