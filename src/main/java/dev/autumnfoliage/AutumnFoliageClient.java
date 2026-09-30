package dev.autumnfoliage;

import dev.autumnfoliage.client.ClientEvents;
import dev.autumnfoliage.client.ClientGameEvents;
import dev.autumnfoliage.client.gui.AutumnSettingsScreen;
import dev.autumnfoliage.compat.DistantHorizonsBridge;
import dev.autumnfoliage.config.AutumnConfig;
import dev.autumnfoliage.network.ServerZoneOverride;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Physical-client setup kept out of the common server classloading path. */
public final class AutumnFoliageClient {
    private AutumnFoliageClient() {}

    public static void init(IEventBus modBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AutumnConfig.SPEC);

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new AutumnSettingsScreen(parent)
                )
        );

        modBus.register(ClientEvents.class);
        MinecraftForge.EVENT_BUS.register(ClientGameEvents.class);
        ServerZoneOverride.setChangeListener(ClientEvents::refreshRendering);
        DistantHorizonsBridge.initIfPresent();
    }
}
