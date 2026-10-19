package dev.councilcraft.mixin;

import dev.councilcraft.respawn.RebornActivity;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Taking a crafted result. The empty method on {@code PlayerEntity} is not the one that runs. */
@Mixin(ServerPlayerEntity.class)
public class RebornCraftMixin {
    @Inject(method = "onRecipeCrafted", at = @At("TAIL"))
    private void councilcraft$craft(Recipe<?> recipe, List<ItemStack> stacks, CallbackInfo ci) {
        RebornActivity.craft((ServerPlayerEntity) (Object) this);
    }
}
