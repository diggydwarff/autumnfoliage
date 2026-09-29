package dev.autumnfoliage.compat;

import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Keeps the core mod free of hard Distant Horizons class references. The DH implementation class
 * is only loaded when Distant Horizons is actually installed.
 */
public final class DistantHorizonsBridge {
    private static final String IMPL =
            "dev.autumnfoliage.compat.distanthorizons.DistantHorizonsCompat";

    private static volatile Method refreshMethod;
    private static volatile boolean initialized;

    private DistantHorizonsBridge() {}

    public static void initIfPresent() {
        if (initialized || !ModList.get().isLoaded("distanthorizons")) {
            return;
        }
        synchronized (DistantHorizonsBridge.class) {
            if (initialized) {
                return;
            }
            try {
                Class<?> type = Class.forName(IMPL);
                type.getMethod("init").invoke(null);
                refreshMethod = type.getMethod("refreshRenderData");
                initialized = true;
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | LinkageError ignored) {
                // DH is optional. If its API changes incompatibly, vanilla rendering still works.
            }
        }
    }

    public static void refreshRenderData() {
        if (!initialized) {
            initIfPresent();
        }
        Method method = refreshMethod;
        if (method == null) {
            return;
        }
        try {
            method.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            // Never let an optional compatibility refresh break the client.
        }
    }
}
