package dev.autumnfoliage.network;

import dev.autumnfoliage.AutumnFoliage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Zero-data optional handshake sent after login. The actual zone values are carried by NeoForge's
 * normal SERVER-config sync; this payload simply tells the client that a server policy is active.
 */
public record ServerConfigActivePayload() implements CustomPacketPayload {
    public static final ServerConfigActivePayload INSTANCE = new ServerConfigActivePayload();
    public static final Type<ServerConfigActivePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AutumnFoliage.MOD_ID, "server_policy_active")
    );
    public static final StreamCodec<ByteBuf, ServerConfigActivePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
