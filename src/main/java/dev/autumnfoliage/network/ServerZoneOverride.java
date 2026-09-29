package dev.autumnfoliage.network;

/**
 * Tracks whether the current connection has an Autumn Foliage server supplying authoritative zones.
 * The change callback is installed only by the physical client entrypoint.
 */
public final class ServerZoneOverride {
    private static volatile boolean active;
    private static volatile Runnable changeListener = () -> {};

    private ServerZoneOverride() {}

    public static boolean isActive() {
        return active;
    }

    public static void activate() {
        active = true;
        changeListener.run();
    }

    public static void clear() {
        if (!active) {
            return;
        }
        active = false;
        changeListener.run();
    }

    public static void setChangeListener(Runnable listener) {
        changeListener = listener == null ? () -> {} : listener;
    }
}
