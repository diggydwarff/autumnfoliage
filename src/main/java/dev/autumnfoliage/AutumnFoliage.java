package dev.autumnfoliage;

import dev.autumnfoliage.client.ClientEvents;
import dev.autumnfoliage.config.AutumnConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(value = AutumnFoliage.MOD_ID, dist = Dist.CLIENT)
public final class AutumnFoliage {
    public static final String MOD_ID = "autumnfoliage";

    public AutumnFoliage(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, AutumnConfig.SPEC);
        modBus.register(ClientEvents.class);
    }
}
