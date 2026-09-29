package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes otherwise-untinted vegetation quads participate in the standard BlockColor pipeline.
 *
 * Minecraft 1.21.5+ replaced the old BakedModel block path with BlockStateModel/BlockModelPart.
 * This wrapper keeps the original behavior: models stay untouched unless the runtime block
 * state is vegetation that is safe to tint as a whole, and already-tinted quads are never changed.
 */
public final class TintForcingBlockStateModel extends DelegateBlockStateModel {
    /*
     * Baked model parts are normally shared. Cache their tinted wrappers by identity so chunk
     * rebuilds do not repeatedly copy the same quad lists. The cache is bounded because custom
     * dynamic models are allowed to manufacture new BlockModelPart instances at runtime.
     */
    private static final int MAX_CACHED_PARTS = 4096;
    private static final Map<BlockModelPart, BlockModelPart> TINTED_PART_CACHE = new IdentityHashMap<>();

    public TintForcingBlockStateModel(BlockStateModel originalModel) {
        super(originalModel);
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockModelPart> parts
    ) {
        int firstAddedPart = parts.size();
        delegate.collectParts(level, pos, state, random, parts);

        if (!AutumnConfig.runtime().forceTintUntintedModels()) {
            return;
        }

        VegetationType type = VegetationClassifier.classify(state);
        if (!VegetationClassifier.canForceTintWholeModel(state, type)) {
            return;
        }

        for (int i = firstAddedPart; i < parts.size(); i++) {
            parts.set(i, tintedPart(parts.get(i)));
        }
    }

    static void clearCache() {
        synchronized (TINTED_PART_CACHE) {
            TINTED_PART_CACHE.clear();
        }
    }

    private static BlockModelPart tintedPart(BlockModelPart original) {
        synchronized (TINTED_PART_CACHE) {
            BlockModelPart cached = TINTED_PART_CACHE.get(original);
            if (cached != null) {
                return cached;
            }

            BlockModelPart wrapped = TintForcingBlockModelPart.wrapIfNeeded(original);
            if (TINTED_PART_CACHE.size() >= MAX_CACHED_PARTS) {
                TINTED_PART_CACHE.clear();
            }
            TINTED_PART_CACHE.put(original, wrapped);
            return wrapped;
        }
    }

    /** Preserves every model-part property and changes only tint index -1 to tint index 0. */
    private static final class TintForcingBlockModelPart implements BlockModelPart {
        private final BlockModelPart delegate;
        private final List<BakedQuad> unculledQuads;
        private final Map<Direction, List<BakedQuad>> culledQuads;

        private TintForcingBlockModelPart(BlockModelPart delegate) {
            this.delegate = delegate;
            this.unculledQuads = forceTint(delegate.getQuads(null));
            this.culledQuads = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                this.culledQuads.put(direction, forceTint(delegate.getQuads(direction)));
            }
        }

        static BlockModelPart wrapIfNeeded(BlockModelPart original) {
            if (!hasUntintedQuad(original.getQuads(null))) {
                boolean found = false;
                for (Direction direction : Direction.values()) {
                    if (hasUntintedQuad(original.getQuads(direction))) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return original;
                }
            }
            return new TintForcingBlockModelPart(original);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? unculledQuads : culledQuads.get(direction);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return delegate.particleIcon();
        }

        @Override
        public @Nullable ChunkSectionLayer getRenderType(BlockState state) {
            return delegate.getRenderType(state);
        }

        @Override
        public TriState ambientOcclusion() {
            return delegate.ambientOcclusion();
        }

        private static boolean hasUntintedQuad(List<BakedQuad> quads) {
            for (BakedQuad quad : quads) {
                if (!quad.isTinted()) {
                    return true;
                }
            }
            return false;
        }

        private static List<BakedQuad> forceTint(List<BakedQuad> quads) {
            if (quads.isEmpty() || !hasUntintedQuad(quads)) {
                return quads;
            }

            List<BakedQuad> out = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                if (quad.isTinted()) {
                    out.add(quad);
                } else {
                    out.add(new BakedQuad(
                            quad.vertices(),
                            0,
                            quad.direction(),
                            quad.sprite(),
                            quad.shade(),
                            quad.lightEmission(),
                            quad.hasAmbientOcclusion()
                    ));
                }
            }
            return List.copyOf(out);
        }
    }
}
