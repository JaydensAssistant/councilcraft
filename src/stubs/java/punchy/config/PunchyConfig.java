package punchy.config;

/**
 * Name only. Compile-time stand-in for Punchy 2.8g's config class, for PPM101 (the Vanilla look also turns
 * Punchy off). Only the members CouncilCraft calls are declared. Not packed into our jar.
 */
public final class PunchyConfig {
    private PunchyConfig() {
    }

    public static Data snapshot() {
        throw new AssertionError();
    }

    public static void applyAndSave(Data data) {
        throw new AssertionError();
    }

    public static final class Data {
        public boolean enableMod;
    }
}
