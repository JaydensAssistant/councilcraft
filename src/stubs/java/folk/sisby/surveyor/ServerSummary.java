package folk.sisby.surveyor;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/** Compile-time stand-in for Surveyor 1.2.4+1.20's class of the same name (PT42). Not packed into our jar. */
public class ServerSummary {
    public static ServerSummary of(MinecraftServer server) {
        throw new UnsupportedOperationException("stub");
    }

    public int groupSize(UUID player) {
        throw new UnsupportedOperationException("stub");
    }
}
