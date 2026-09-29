package dev.autumnfoliage.client;

import dev.autumnfoliage.AutumnFoliage;
import dev.autumnfoliage.compat.DistantHorizonsBridge;
import dev.autumnfoliage.config.AutumnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ClientEvents {
    private static final Map<Block, BlockColor> ORIGINAL_BLOCK_COLORS = new IdentityHashMap<>();

    private ClientEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        ORIGINAL_BLOCK_COLORS.clear();
        ORIGINAL_BLOCK_COLORS.putAll(event.getBlockColors().blockColors);

        BlockColor autumnAware = (state, level, pos, tintIndex) -> {
            BlockColor original = ORIGINAL_BLOCK_COLORS.get(state.getBlock());
            int baseColor = original == null ? -1 : original.getColor(state, level, pos, tintIndex);
            return AutumnColorizer.color(state, level, pos, baseColor);
        };

        Block[] allBlocks = BuiltInRegistries.BLOCK.stream().toArray(Block[]::new);
        event.register(autumnAware, allBlocks);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // The event runs on a model-reload worker thread, so only touch the provided model map here.
        // Runtime vegetation classification happens later when getQuads receives a BlockState.
        event.getModels().replaceAll((location, model) ->
                model instanceof TintForcingBakedModel ? model : new TintForcingBakedModel(model));
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (AutumnFoliage.MOD_ID.equals(event.getConfig().getModId())) {
            refreshRendering();
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (AutumnFoliage.MOD_ID.equals(event.getConfig().getModId())) {
            refreshRendering();
        }
    }

    public static void refreshRendering() {
        AutumnConfig.refresh();
        VegetationClassifier.clearCache();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.levelRenderer != null) {
                minecraft.levelRenderer.allChanged();
            }
            DistantHorizonsBridge.refreshRenderData();
        });
    }
}
