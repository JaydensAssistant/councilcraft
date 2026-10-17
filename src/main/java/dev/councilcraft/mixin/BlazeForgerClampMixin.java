package dev.councilcraft.mixin;

import dev.councilcraft.enchant.ForgerLevels;
import dev.councilcraft.enchant.PrintedCopies;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * S177 / S207. After the Blaze Forger has chosen a level, clamp it.
 * Two inputs that are both at or under the vanilla max cannot produce a
 * level above that max. Only Sharpness, Power, Blast Protection and Protection
 * may sit one above the max, and only when an input was already above it.
 *
 * <p>Extracting an enchant from a printed stack marks the template that
 * receives it. The source stack is the same object, so it keeps its own mark.
 */
@Pseudo
@Mixin(targets = "plus.dragons.createenchantmentindustry.common.processing.forger.BlazeForgerInventory")
public abstract class BlazeForgerClampMixin {
    private static final ThreadLocal<Map<Enchantment, Integer>> BASE = new ThreadLocal<>();
    private static final ThreadLocal<Map<Enchantment, Integer>> ADDITION = new ThreadLocal<>();

    @Shadow
    private static void setEnchantments(ItemStack stack, Map<Enchantment, Integer> enchantments) {
        throw new AssertionError();
    }

    @Inject(method = "applyEnchantments", at = @At("HEAD"))
    private void councilcraft$capture(
        ItemStack base,
        Map<Enchantment, Integer> baseEnchantments,
        Map<Enchantment, Integer> additionEnchantments,
        CallbackInfoReturnable<?> cir
    ) {
        BASE.set(baseEnchantments);
        ADDITION.set(additionEnchantments);
    }

    @Inject(method = "applyEnchantments", at = @At("RETURN"))
    private void councilcraft$clear(
        ItemStack base,
        Map<Enchantment, Integer> baseEnchantments,
        Map<Enchantment, Integer> additionEnchantments,
        CallbackInfoReturnable<?> cir
    ) {
        BASE.remove();
        ADDITION.remove();
    }

    @Redirect(
        method = "applyEnchantments",
        at = @At(
            value = "INVOKE",
            target = "Lplus/dragons/createenchantmentindustry/common/processing/forger/BlazeForgerInventory;setEnchantments(Lnet/minecraft/item/ItemStack;Ljava/util/Map;)V"
        )
    )
    private void councilcraft$clamp(ItemStack stack, Map<Enchantment, Integer> result) {
        Map<Enchantment, Integer> base = BASE.get();
        Map<Enchantment, Integer> addition = ADDITION.get();
        if (base != null && addition != null) {
            for (Enchantment enchantment : new java.util.ArrayList<>(result.keySet())) {
                Identifier id = Registries.ENCHANTMENT.getId(enchantment);
                if (id == null) {
                    continue;
                }
                int level = result.getOrDefault(enchantment, 0);
                int clamped = ForgerLevels.clamp(
                    ForgerLevels.mayExceedVanillaMax(id.toString()),
                    enchantment.getMaxLevel(),
                    base.getOrDefault(enchantment, 0),
                    addition.getOrDefault(enchantment, 0),
                    level
                );
                if (clamped != level) {
                    result.put(enchantment, clamped);
                }
            }
        }
        setEnchantments(stack, result);
    }

    @Inject(method = "calculateExtract", at = @At("RETURN"))
    private void councilcraft$carryPrinted(ItemStack base, ItemStack addition, CallbackInfoReturnable<?> cir) {
        Object result = cir.getReturnValue();
        if (result == null || !PrintedCopies.isPrinted(base)) {
            return;
        }
        try {
            Object status = result.getClass().getMethod("status").invoke(result);
            if (status != null && "READY".equals(status.toString())) {
                PrintedCopies.mark(addition);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "CouncilCraft hook failed: printed copies (S207). Blaze Forger result status is missing or was renamed.",
                e
            );
        }
    }
}
