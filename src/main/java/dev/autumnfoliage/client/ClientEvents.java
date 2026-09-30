package dev.autumnfoliage.client;

import dev.autumnfoliage.AutumnFoliage;
import dev.autumnfoliage.compat.DistantHorizonsBridge;
import dev.autumnfoliage.config.AutumnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class ClientEvents {
    private static final Map<Block, BlockColor> ORIGINAL_BLOCK_COLORS = new IdentityHashMap<>();
    private static final Set<Block> REGISTERED_AUTUMN_BLOCKS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private static BlockColors activeBlockColors;

    private static final BlockColor AUTUMN_AWARE_COLOR = (state, level, pos, tintIndex) -> {
        BlockColor original = ORIGINAL_BLOCK_COLORS.get(state.getBlock());
        int baseColor = original == null ? -1 : original.getColor(state, level, pos, tintIndex);
        return AutumnColorizer.color(state, level, pos, baseColor, tintIndex);
    };

    private ClientEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColors blockColors = event.getBlockColors();

        // Snapshot every provider before Autumn Foliage replaces any of them so our wrapper can
        // always delegate back to the real vanilla/mod provider when a block is outside autumn,
        // force-excluded, tropical-disabled, etc.
        ORIGINAL_BLOCK_COLORS.clear();
        ORIGINAL_BLOCK_COLORS.putAll(blockColors.blockColors);
        REGISTERED_AUTUMN_BLOCKS.clear();
        activeBlockColors = blockColors;

        VegetationClassifier.clearCache();

        // IMPORTANT: go through RegisterColorHandlersEvent/BlockColors.register instead of writing
        // directly to BlockColors.blockColors. Sodium hooks the normal registration path to mirror
        // block color providers into its renderer. Direct map writes work in vanilla but Sodium will
        // never see them, which makes all autumn tinting disappear.
        Block[] vegetationBlocks = BuiltInRegistries.BLOCK.stream()
                .filter(ClientEvents::shouldHaveAutumnProvider)
                .toArray(Block[]::new);

        if (vegetationBlocks.length > 0) {
            event.register(AUTUMN_AWARE_COLOR, vegetationBlocks);
            Collections.addAll(REGISTERED_AUTUMN_BLOCKS, vegetationBlocks);
        }
    }

    /**
     * Returns true only for blocks that Autumn Foliage can currently classify as vegetation.
     * This keeps the provider hook away from unrelated tinted blocks such as water, redstone,
     * machine displays, copycats, food/storage blocks, etc.
     */
    private static boolean shouldHaveAutumnProvider(Block block) {
        return VegetationClassifier.classify(block.defaultBlockState()) != VegetationType.NONE;
    }

    /**
     * Adds any blocks that became eligible after a config change (for example Force Include).
     * Providers are intentionally not unregistered during the session: Force Exclude is handled
     * inside AutumnColorizer/VegetationClassifier, which returns the original provider color.
     * Keeping the small already-managed set registered avoids renderer cache desynchronization.
     */
    private static void registerNewlyEligibleBlocks() {
        BlockColors blockColors = activeBlockColors;
        if (blockColors == null) {
            return;
        }

        Block[] newlyEligible = BuiltInRegistries.BLOCK.stream()
                .filter(block -> !REGISTERED_AUTUMN_BLOCKS.contains(block))
                .filter(ClientEvents::shouldHaveAutumnProvider)
                .toArray(Block[]::new);

        if (newlyEligible.length == 0) {
            return;
        }

        // Use the public registration path here too so Sodium and other renderers observe the
        // change just as they do during RegisterColorHandlersEvent.
        blockColors.register(AUTUMN_AWARE_COLOR, newlyEligible);
        Collections.addAll(REGISTERED_AUTUMN_BLOCKS, newlyEligible);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // 1.21.8 block rendering uses BlockStateModel rather than the pre-1.21.5 BakedModel path.
        // Modify only block-state models: item models and custom item renderers remain untouched.
        // Runtime vegetation classification still happens during collectParts(), where NeoForge
        // supplies the actual BlockState/world context used by the chunk mesher.
        TintForcingBlockStateModel.clearCache();
        event.getBakingResult().blockStateModels().replaceAll((state, model) ->
                model instanceof TintForcingBlockStateModel ? model : new TintForcingBlockStateModel(model)
        );
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
        // Config Loading can fire during NeoForge startup before Minecraft has constructed its
        // client singleton. Refresh the config/classification state immediately, but only touch
        // renderers once a live client exists.
        AutumnConfig.refresh();
        VegetationClassifier.clearCache();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        minecraft.execute(() -> {
            registerNewlyEligibleBlocks();

            if (minecraft.levelRenderer != null) {
                minecraft.levelRenderer.allChanged();
            }
            DistantHorizonsBridge.refreshRenderData();
        });
    }
}
