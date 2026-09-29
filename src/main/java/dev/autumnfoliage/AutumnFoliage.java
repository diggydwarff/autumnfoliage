package dev.autumnfoliage;

import dev.autumnfoliage.config.AutumnServerConfig;
import dev.autumnfoliage.network.NetworkEvents;
import dev.autumnfoliage.network.ServerGameEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Common entrypoint. The mod can be installed on a dedicated server, but it is not required there.
 * A server installation only supplies optional, authoritative coordinate-zone configuration.
 */
@Mod(AutumnFoliage.MOD_ID)
public final class AutumnFoliage {
    public static final String MOD_ID = "autumnfoliage";

    public AutumnFoliage(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, AutumnServerConfig.SPEC);
        modBus.addListener(NetworkEvents::registerPayloads);
        NeoForge.EVENT_BUS.register(ServerGameEvents.class);
    }
}
