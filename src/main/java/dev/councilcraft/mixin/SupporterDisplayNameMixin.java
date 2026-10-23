package dev.councilcraft.mixin;

import dev.councilcraft.supporter.Supporters;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM102. Server-side players' display name (used by chat, including No Chat Reports' system-message chat) gets
 * the donor ★ badge, role prefix and name color. Client-side player entities are untouched.
 */
@Mixin(PlayerEntity.class)
public abstract class SupporterDisplayNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void councilcraft$supporterName(CallbackInfoReturnable<Text> cir) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            Text decorated = Supporters.decorate(player.getUuid(), cir.getReturnValue());
            if (decorated != null) {
                cir.setReturnValue(decorated);
            }
        }
    }
}
