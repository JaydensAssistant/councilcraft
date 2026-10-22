package dev.councilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.councilcraft.compat.InventorioParagliders;
import dev.councilcraft.compat.ParagliderHands;
import dev.councilcraft.rules.ParagliderHand;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.api.item.Paraglider;
import tictim.paraglider.api.movement.ParagliderPlayerStates;
import tictim.paraglider.impl.movement.ServerPlayerMovement;

/**
 * PPM70. Server side of an offhand glide in Paragliders 20.1.4 {@code ServerPlayerMovement}:
 * <ul>
 * <li>durability: {@code applyMovement} damages the main-hand stack; it damages the stack in use;</li>
 * <li>Paragliding flag: {@code update} sets it on inventory slot {@code i} when {@code i} is the selected
 * hotbar slot; it is set on the stack in use instead (main hand, or offhand, slot 40 without
 * Inventorio), cleared on every other slot;</li>
 * <li>Inventorio's utility belt is outside those slots, so its stacks get the same rule at the end of
 * {@code update} ({@link InventorioParagliders}).</li>
 * </ul>
 */
@Mixin(value = ServerPlayerMovement.class, remap = false)
public abstract class ParagliderOffhandMovementMixin {
    @Unique
    private static final boolean COUNCILCRAFT$INVENTORIO = FabricLoader.getInstance().isModLoaded("inventorio");

    @WrapOperation(method = "applyMovement", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerPlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;",
        remap = true))
    private ItemStack councilcraft$damageGliderInUse(ServerPlayerEntity player, Operation<ItemStack> original) {
        return ParagliderHands.glider(player, original.call(player));
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE",
        target = "Ltictim/paraglider/api/item/Paraglider;setParagliding(Lnet/minecraft/item/ItemStack;Z)V",
        remap = true)) // remaps the ItemStack in the descriptor
    private void councilcraft$flagGliderInUse(Paraglider item, ItemStack stack, boolean selectedAndGliding, Operation<Void> original) {
        ServerPlayerEntity player = ((ServerPlayerMovement) (Object) this).player();
        original.call(item, stack, ParagliderHand.flagged(stack == ParagliderHands.glider(player), councilcraft$gliding()));
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void councilcraft$flagBelt(CallbackInfo ci) {
        if (COUNCILCRAFT$INVENTORIO) {
            ServerPlayerEntity player = ((ServerPlayerMovement) (Object) this).player();
            InventorioParagliders.syncBeltFlags(player, ParagliderHands.glider(player), councilcraft$gliding());
        }
    }

    @Unique
    private boolean councilcraft$gliding() {
        return ((ServerPlayerMovement) (Object) this).state().has(ParagliderPlayerStates.Flags.FLAG_PARAGLIDING);
    }
}
