package dev.autumnfoliage.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkEvents {
    private NetworkEvents() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(
                ServerConfigActivePayload.TYPE,
                ServerConfigActivePayload.STREAM_CODEC,
                (payload, context) -> ServerZoneOverride.activate()
        );
    }
}
