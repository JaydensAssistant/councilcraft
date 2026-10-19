package dev.councilcraft.mixin;

import dev.councilcraft.handshake.LoginHandshake;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric calls {@code acceptPlayer} from the login tick only after every login
 * query has been answered and every query future has finished. That is the
 * first moment a disconnect packet is delivered instead of reset.
 */
@Mixin(ServerLoginNetworkHandler.class)
public class LoginAcceptMixin {
    @Inject(method = "acceptPlayer", at = @At("HEAD"), cancellable = true)
    private void councilcraft$disconnectHeldRejection(CallbackInfo ci) {
        String message = LoginHandshake.takeRejection((ServerLoginNetworkHandler) (Object) this);
        if (message != null) {
            ((ServerLoginNetworkHandler) (Object) this).disconnect(Text.literal(message));
            ci.cancel();
        }
    }
}
