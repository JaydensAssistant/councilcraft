package folk.sisby.surveyor;

import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

/** Compile-time stand-in for Surveyor 1.2.4+1.20's class of the same name (PT42). Not packed into our jar. */
public class Surveyor {
    public static UUID getUuid(ServerPlayerEntity player) {
        throw new UnsupportedOperationException("stub");
    }
}
