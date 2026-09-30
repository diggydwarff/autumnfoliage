package dev.autumnfoliage.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/** Server hooks are harmless when the mod is only installed on a client. */
public final class ServerGameEvents {
    private ServerGameEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // The channel is optional, so only send when the remote side actually negotiated it.
        if (NetworkEvents.CHANNEL.isRemotePresent(player.connection.connection)) {
            NetworkEvents.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    ServerConfigActivePayload.INSTANCE
            );
        }
    }
}
