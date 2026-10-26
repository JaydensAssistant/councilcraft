package dev.councilcraft.mixin;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import dev.councilcraft.compat.DciGuard;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PT18. The /discord command's requirement (McCommandDiscord's lambda$new$2) reads DiscordIntegration.INSTANCE
 * without a null check; the command tree is sent on join, so a missing bot kicked every joining player. While
 * the bot is missing, the requirement says no (the command is hidden). Pinned to dcintegration-fabric 3.0.7
 * because it targets a compiler-named lambda.
 */
@Pseudo
@Mixin(targets = "de.erdbeerbaerlp.dcintegration.fabric.command.McCommandDiscord", remap = false)
public abstract class DciCommandGuardMixin {
    @Inject(method = "lambda$new$2", at = @At("HEAD"), cancellable = true, remap = false)
    private static void councilcraft$hideWithoutBot(ServerCommandSource source, CallbackInfoReturnable<Boolean> cir) {
        if (DiscordIntegration.INSTANCE == null) {
            DciGuard.warnOnce();
            cir.setReturnValue(false);
        }
    }
}
