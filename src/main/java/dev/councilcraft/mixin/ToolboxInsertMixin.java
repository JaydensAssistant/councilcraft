package dev.councilcraft.mixin;

import dev.councilcraft.CouncilCraftRules;
import dev.councilcraft.rules.ToolboxFilter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S234 / S235. Create 6.0.8.1 decides toolbox inserts in
 * {@code ToolboxInventory.isItemValid}. That method already rejects items
 * that cannot nest, which is how sacks stay out. This refuses anything that
 * is not in {@code councilcraft:toolbox_contents}.
 *
 * <p>The target is named as a string, and marked {@code @Pseudo}, because the class
 * extends a Porting Lib type nested inside the Create jar. The annotation processor
 * cannot load that superclass, so it cannot see the target while compiling.
 * {@code @Pseudo} would skip a missing class at runtime. {@link CouncilCraftMixinPlugin}
 * refuses that: if Create is loaded and this class is gone, startup throws.
 * If the class is there and {@code isItemValid} was renamed, mixin {@code require}
 * still crashes and the log names this class.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.equipment.toolbox.ToolboxInventory", remap = false)
public class ToolboxInsertMixin {
    private static final TagKey<Item> CONTENTS = TagKey.of(
        RegistryKeys.ITEM,
        new Identifier(CouncilCraftRules.MOD_ID, CouncilCraftRules.TOOLBOX_TAG_PATH)
    );

    @Inject(
        method = "isItemValid(ILnet/fabricmc/fabric/api/transfer/v1/item/ItemVariant;I)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void councilcraft$whitelist(int slot, ItemVariant variant, int amount, CallbackInfoReturnable<Boolean> cir) {
        boolean inWhitelist = Registries.ITEM.getEntry(variant.getItem()).isIn(CONTENTS);
        if (!ToolboxFilter.allows(variant.isBlank(), inWhitelist)) {
            cir.setReturnValue(false);
        }
    }
}
