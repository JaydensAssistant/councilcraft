package dev.councilcraft.mixin;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.fabric.DiscordIntegrationMod;
import dev.councilcraft.compat.DciGuard;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PT18. Discord Integration 3.0.7's leave hook (its NetworkHandlerMixin, HEAD of onDisconnected) skips itself
 * when DiscordIntegrationMod.stopped is set, and otherwise calls LinkManager.isPlayerLinked, which throws when
 * the bot never started (no token, intents off). While the bot is missing, raise that flag around the
 * disconnect so DCI's own guard skips the hook, then put it back. Priority 900 puts this HEAD callback before
 * DCI's (default 1000); checked in the exported bytecode. Pinned to dcintegration-fabric 3.0.7.
 */
@Mixin(value = ServerPlayNetworkHandler.class, priority = 900)
public abstract class DciLeaveGuardMixin {
    @Unique
    private boolean councilcraft$raisedStopped;

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void councilcraft$skipDciWithoutBot(Text reason, CallbackInfo ci) {
        if (DiscordIntegration.INSTANCE == null && !DiscordIntegrationMod.stopped) {
            DciGuard.warnOnce();
            DiscordIntegrationMod.stopped = true;
            this.councilcraft$raisedStopped = true;
        }
    }

    @Inject(method = "onDisconnected", at = @At("RETURN"))
    private void councilcraft$restoreDciFlag(Text reason, CallbackInfo ci) {
        if (this.councilcraft$raisedStopped) {
            this.councilcraft$raisedStopped = false;
            DiscordIntegrationMod.stopped = false;
        }
    }
}
