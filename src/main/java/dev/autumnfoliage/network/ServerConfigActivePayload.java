package dev.autumnfoliage.network;

import dev.autumnfoliage.AutumnFoliage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * Zero-data optional handshake sent after login. The actual zone values are carried by NeoForge's
 * normal SERVER-config sync; this payload simply tells the client to prefer them over local zones.
 */
public record ServerConfigActivePayload() implements CustomPacketPayload {
    public static final ServerConfigActivePayload INSTANCE = new ServerConfigActivePayload();
    public static final Type<ServerConfigActivePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AutumnFoliage.MOD_ID, "server_zones_active")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerConfigActivePayload> STREAM_CODEC =
            NeoForgeStreamCodecs.uncheckedUnit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
