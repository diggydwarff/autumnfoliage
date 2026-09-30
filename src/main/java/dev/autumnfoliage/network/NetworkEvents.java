package dev.autumnfoliage.network;

import dev.autumnfoliage.AutumnFoliage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkEvents {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(AutumnFoliage.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(NetworkEvents::acceptsVersion)
            .serverAcceptedVersions(NetworkEvents::acceptsVersion)
            .simpleChannel();

    private NetworkEvents() {}

    private static boolean acceptsVersion(String version) {
        if (PROTOCOL.equals(version)) {
            return true;
        }

        // Keep the channel optional: Autumn Foliage is allowed to be client-only or absent
        // on either side. Forge passes sentinel strings for missing/vanilla peers. Prefix
        // checks are intentionally used because the exact sentinel text has varied slightly
        // across Forge revisions.
        return version != null
                && (version.startsWith("ABSENT") || version.startsWith("ALLOWVANILLA"));
    }

    public static void register() {
        CHANNEL.messageBuilder(ServerConfigActivePayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NetworkEvents::encode)
                .decoder(NetworkEvents::decode)
                .consumerMainThread((payload, context) -> ServerZoneOverride.activate())
                .add();
    }

    private static void encode(ServerConfigActivePayload payload, FriendlyByteBuf buffer) {
        // Presence-only packet: there is no payload data to encode.
    }

    private static ServerConfigActivePayload decode(FriendlyByteBuf buffer) {
        return ServerConfigActivePayload.INSTANCE;
    }
}
