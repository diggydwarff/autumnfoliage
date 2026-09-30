package dev.autumnfoliage.client;

import dev.autumnfoliage.AutumnFoliage;
import dev.autumnfoliage.compat.DistantHorizonsBridge;
import dev.autumnfoliage.config.AutumnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ClientEvents {
    private static final Map<Block, BlockColor> ORIGINAL_BLOCK_COLORS = new IdentityHashMap<>();

    private ClientEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        ORIGINAL_BLOCK_COLORS.clear();
        Map<net.minecraft.core.Holder.Reference<Block>, BlockColor> registeredColors =
                ObfuscationReflectionHelper.getPrivateValue(
                        BlockColors.class,
                        event.getBlockColors(),
                        "f_92571_"
                );
        if (registeredColors != null) {
            // Forge 1.20.1 stores vanilla/mod block colors by Holder.Reference. Do not retain
            // those holder objects as IdentityHashMap keys: the holder instance used later by a
            // Block lookup is not guaranteed to be the exact same Java object. Collapse the
            // snapshot back to stable Block instances, matching the newer NeoForge ports.
            registeredColors.forEach((holder, color) -> {
                if (holder != null && color != null) {
                    ORIGINAL_BLOCK_COLORS.put(holder.value(), color);
                }
            });
        }

        BlockColor autumnAware = (state, level, pos, tintIndex) -> {
            BlockColor original = ORIGINAL_BLOCK_COLORS.get(state.getBlock());
            int baseColor = original == null ? -1 : original.getColor(state, level, pos, tintIndex);
            return AutumnColorizer.color(state, level, pos, baseColor, tintIndex);
        };

        Block[] allBlocks = BuiltInRegistries.BLOCK.stream().toArray(Block[]::new);
        event.register(autumnAware, allBlocks);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // The event runs on a model-reload worker thread, so only touch the provided model map here.
        // Runtime vegetation classification happens later when getQuads receives a BlockState.
        //
        // IMPORTANT: only wrap block-state models. Item models use the "inventory" variant and
        // many mods (notably Create) rely on the baked model retaining a concrete custom type.
        // Wrapping those models breaks their renderer casts even though state == null means we
        // would never tint the item anyway. Standalone/additional models are skipped for the
        // same reason: they are not needed for world foliage tinting and may have custom renderers.
        event.getModels().replaceAll((location, model) -> {
            if (model instanceof TintForcingBakedModel) {
                return model;
            }
            if (!(location instanceof ModelResourceLocation modelLocation)) {
                return model;
            }
            String variant = modelLocation.getVariant();
            if ("inventory".equals(variant)) {
                return model;
            }
            return new TintForcingBakedModel(model);
        });
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
        if (minecraft == null) {
            return;
        }
        minecraft.execute(() -> {
            if (minecraft.levelRenderer != null) {
                minecraft.levelRenderer.allChanged();
            }
            DistantHorizonsBridge.refreshRenderData();
        });
    }
}
