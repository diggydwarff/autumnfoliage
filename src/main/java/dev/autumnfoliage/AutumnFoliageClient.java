package dev.autumnfoliage;

import dev.autumnfoliage.client.ClientEvents;
import dev.autumnfoliage.client.ClientGameEvents;
import dev.autumnfoliage.config.AutumnConfig;
import dev.autumnfoliage.network.ServerZoneOverride;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Physical-client entrypoint containing rendering setup and the built-in NeoForge config screen. */
@Mod(value = AutumnFoliage.MOD_ID, dist = Dist.CLIENT)
public final class AutumnFoliageClient {
    public AutumnFoliageClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, AutumnConfig.SPEC);
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new ConfigurationScreen(modContainer, parent)
        );

        modBus.register(ClientEvents.class);
        NeoForge.EVENT_BUS.register(ClientGameEvents.class);
        ServerZoneOverride.setChangeListener(ClientEvents::refreshRendering);
    }
}
