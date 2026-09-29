package dev.autumnfoliage.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Server hooks are harmless when the mod is only installed on a client. */
public final class ServerGameEvents {
    private ServerGameEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (NetworkRegistry.hasChannel(player.connection, ServerConfigActivePayload.TYPE.id())) {
            PacketDistributor.sendToPlayer(player, ServerConfigActivePayload.INSTANCE);
        }
    }
}
