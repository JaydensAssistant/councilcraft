package dev.councilcraft.compat;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import dev.councilcraft.advancement.MilestoneRules;
import dev.councilcraft.advancement.Milestones;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * PT42 Testing, Testing. Simple Voice Chat plugin (fabric.mod.json "voicechat" entrypoint; ignored when Simple Voice
 * Chat is absent). The first microphone packet a player sends outside a group (proximity chat) grants the
 * advancement. The event runs on the voice thread, so the grant is handed to the server thread once per session.
 */
public final class CouncilCraftVoicechat implements VoicechatPlugin {
    private static final Set<UUID> HANDLED = ConcurrentHashMap.newKeySet();

    @Override
    public String getPluginId() {
        return "councilcraft";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, CouncilCraftVoicechat::onMicrophone);
    }

    private static void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.isInGroup()) {
            return;
        }
        if (!(sender.getPlayer().getPlayer() instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!HANDLED.add(player.getUuid())) {
            return;
        }
        player.getServer().execute(() -> {
            if (player.isRemoved()) {
                HANDLED.remove(player.getUuid());
            } else {
                Milestones.grant(player, MilestoneRules.TESTING_TESTING);
            }
        });
    }
}
