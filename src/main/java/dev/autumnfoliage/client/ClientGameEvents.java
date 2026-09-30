package dev.autumnfoliage.client;

import dev.autumnfoliage.network.ServerZoneOverride;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** Main NeoForge event-bus hooks that can change runtime coloring/classification. */
public final class ClientGameEvents {
    private static int calendarCheckTicks;

    private ClientGameEvents() {}

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        VegetationClassifier.clearCache();
        refreshChunks();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // The next server may not have the mod. Return to the client's local fallback policy.
        ServerZoneOverride.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // The calendar only changes once per day. Check roughly once per second rather than doing
        // date/time work inside the hot block-color path. If midnight rolls over, rebuild nearby
        // chunks and DH render data so the new day's blend is actually visible.
        if (++calendarCheckTicks < 20) {
            return;
        }
        calendarCheckTicks = 0;
        if (SeasonalTiming.updateCurrentDate() && dev.autumnfoliage.config.AutumnConfig.runtime().calendarTimingEnabled()) {
            ClientEvents.refreshRendering();
        }
    }

    private static void refreshChunks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.execute(() -> {
            if (minecraft.levelRenderer != null) {
                minecraft.levelRenderer.allChanged();
            }
        });
    }
}
