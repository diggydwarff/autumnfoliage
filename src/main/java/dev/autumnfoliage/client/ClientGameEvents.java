package dev.autumnfoliage.client;

import dev.autumnfoliage.network.ServerZoneOverride;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** Main NeoForge event-bus hooks that can change runtime coloring/classification. */
public final class ClientGameEvents {
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
