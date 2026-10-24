package dev.councilcraft.mixin;

import dev.councilcraft.supporter.Supporters;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PPM102. Tab-list name: badge, prefix and colored name (PPM107: every player, at least the default blue name).
 * Vanilla returns null (the client then shows the team-decorated name); this leaves a name another mod set
 * alone, and returns null before the supporter data is loaded. Discord Integration relays this name.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class SupporterListNameMixin {
    @Inject(method = "getPlayerListName", at = @At("RETURN"), cancellable = true)
    private void councilcraft$supporterListName(CallbackInfoReturnable<Text> cir) {
        if (cir.getReturnValue() == null) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            Text decorated = Supporters.decorate(player.getUuid(),
                Team.decorateName(player.getScoreboardTeam(), player.getName()));
            if (decorated != null) {
                cir.setReturnValue(decorated);
            }
        }
    }
}
