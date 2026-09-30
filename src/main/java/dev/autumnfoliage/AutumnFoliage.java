package dev.autumnfoliage;

import dev.autumnfoliage.config.AutumnServerConfig;
import dev.autumnfoliage.network.NetworkEvents;
import dev.autumnfoliage.network.ServerGameEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge 1.20.1 common entrypoint. The mod remains optional on dedicated servers; when present on
 * the server it can provide authoritative coordinate-zone settings to clients that also have it.
 */
@Mod(AutumnFoliage.MOD_ID)
public final class AutumnFoliage {
    public static final String MOD_ID = "autumnfoliage";

    public AutumnFoliage() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext loadingContext = ModLoadingContext.get();

        loadingContext.registerConfig(ModConfig.Type.SERVER, AutumnServerConfig.SPEC);

        NetworkEvents.register();
        MinecraftForge.EVENT_BUS.register(ServerGameEvents.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AutumnFoliageClient.init(modBus));
    }
}
