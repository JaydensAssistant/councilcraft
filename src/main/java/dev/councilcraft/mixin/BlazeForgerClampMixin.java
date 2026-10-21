package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.councilcraft.enchant.ForgerLevels;
import dev.councilcraft.enchant.PrintedCopies;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import plus.dragons.createenchantmentindustry.common.processing.forger.BlazeForgerInventory;
import plus.dragons.createenchantmentindustry.common.processing.forger.BlazeForgerMode;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S177 / S207. Clamp the merged level at every store, before Create
 * Enchantment Industry compares it with the level already on the item.
 * A result that clamps back to that level fails the "would not improve"
 * check, so the second item and the XP are not consumed.
 *
 * <p>Two inputs that are both at or under the vanilla max cannot produce a
 * level above that max. Only Sharpness, Power, Blast Protection and Protection
 * may sit one above the max, and only when an input was already above it.
 *
 * <p>PPM51. Extract only takes enchanted books, and any Forger result that involves a printed input is printed.
 */
@Pseudo
@Mixin(targets = "plus.dragons.createenchantmentindustry.common.processing.forger.BlazeForgerInventory")
public abstract class BlazeForgerClampMixin {
    @ModifyVariable(method = "applyEnchantments", name = "resultLevel", at = @At("STORE"))
    private int councilcraft$clampResult(
        int resultLevel,
        @Local(name = "baseLevel") int baseLevel,
        @Local(name = "additionLevel") int additionLevel,
        @Local(name = "enchantment") Enchantment enchantment
    ) {
        Identifier id = Registries.ENCHANTMENT.getId(enchantment);
        if (id == null) {
            return resultLevel;
        }
        return ForgerLevels.clamp(
            ForgerLevels.mayExceedVanillaMax(id.toString()),
            enchantment.getMaxLevel(),
            baseLevel,
            additionLevel,
            resultLevel
        );
    }

    /**
     * PPM51 / PM2b(b). Extract only accepts enchanted books (book splitting). Extracting from gear or a template
     * onto a blank template and applying that to a book would mint a new original book.
     */
    @Inject(method = "calculateExtract", at = @At("HEAD"), cancellable = true)
    private void councilcraft$extractBooksOnly(ItemStack base, ItemStack addition,
                                              CallbackInfoReturnable<BlazeForgerInventory.Result> cir) {
        if (!base.isOf(Items.ENCHANTED_BOOK)) {
            cir.setReturnValue(BlazeForgerInventory.Result.invalid(BlazeForgerMode.EXTRACT,
                Text.translatable("councilcraft.forger.extract_books_only")));
        }
    }

    /**
     * PPM51 copy-wins for every Forger mode (merge, apply, extract). When either input is printed, every output
     * that carries enchantments is printed. A plain book left over from splitting a single-enchant book stays plain.
     * The block entity copies these result stacks into its slots, so marking them here covers preview and craft.
     */
    @Inject(method = "calculateResult", at = @At("RETURN"))
    private void councilcraft$copyWins(ItemStack baseInput, ItemStack additionInput,
                                       CallbackInfoReturnable<BlazeForgerInventory.Result> cir) {
        BlazeForgerInventory.Result result = cir.getReturnValue();
        if (result == null || result.status() != BlazeForgerInventory.Status.READY
            || !PrintedCopies.anyPrinted(baseInput, additionInput)) {
            return;
        }
        councilcraft$markEnchanted(result.primaryOutput());
        councilcraft$markEnchanted(result.secondaryOutput());
    }

    @Unique
    private static void councilcraft$markEnchanted(ItemStack stack) {
        if (PrintedCopies.carriesEnchantments(stack)) {
            PrintedCopies.mark(stack);
        }
    }
}
