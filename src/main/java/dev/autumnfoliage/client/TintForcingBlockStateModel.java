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
    private static final Map<BlockModelPart, BlockModelPart> BAMBOO_PART_CACHE = new IdentityHashMap<>();

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

        // Bamboo needs two explicit tint channels. Unlike normal leaf blocks, vanilla bamboo's
        // textures are already green and are not reliably pre-tagged with foliage tint indices.
        // Classify each bamboo quad by its sprite: stalk texture -> index 1, everything else in the
        // living bamboo model (small/large/single leaves and the sapling stage) -> index 0. The
        // colorizer gives both channels bamboo-specific smooth palettes rather than generic tree
        // family colors, avoiding green-looking gold patches and hard color-family seams.
        // This affects only minecraft:bamboo / bamboo_sapling, never bamboo building blocks.
        if (VegetationClassifier.isBambooPlant(state)) {
            if (type == VegetationType.NONE ||
                    (type == VegetationType.TROPICAL && !AutumnConfig.runtime().autumnalTropics())) {
                return;
            }
            for (int i = firstAddedPart; i < parts.size(); i++) {
                parts.set(i, bambooTintedPart(parts.get(i)));
            }
            return;
        }

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
        synchronized (BAMBOO_PART_CACHE) {
            BAMBOO_PART_CACHE.clear();
        }
    }

    private static BlockModelPart tintedPart(BlockModelPart original) {
        synchronized (TINTED_PART_CACHE) {
            BlockModelPart cached = TINTED_PART_CACHE.get(original);
            if (cached != null) {
                return cached;
            }

            BlockModelPart wrapped = TintForcingBlockModelPart.wrapIfNeeded(original, 0);
            if (TINTED_PART_CACHE.size() >= MAX_CACHED_PARTS) {
                TINTED_PART_CACHE.clear();
            }
            TINTED_PART_CACHE.put(original, wrapped);
            return wrapped;
        }
    }


    private static BlockModelPart bambooTintedPart(BlockModelPart original) {
        synchronized (BAMBOO_PART_CACHE) {
            BlockModelPart cached = BAMBOO_PART_CACHE.get(original);
            if (cached != null) {
                return cached;
            }

            BlockModelPart wrapped = BambooTintBlockModelPart.wrap(original);
            if (BAMBOO_PART_CACHE.size() >= MAX_CACHED_PARTS) {
                BAMBOO_PART_CACHE.clear();
            }
            BAMBOO_PART_CACHE.put(original, wrapped);
            return wrapped;
        }
    }

    /**
     * Bamboo's textures are pre-colored green, so relying on vanilla tint metadata is not enough.
     * Assign a tint index to every quad in the living bamboo model based on the sprite it actually
     * uses. This also covers the small leaf/node details that visually read as foliage but live in
     * separate multipart model pieces.
     */
    private static final class BambooTintBlockModelPart implements BlockModelPart {
        private final BlockModelPart delegate;
        private final List<BakedQuad> unculledQuads;
        private final Map<Direction, List<BakedQuad>> culledQuads;

        private BambooTintBlockModelPart(BlockModelPart delegate) {
            this.delegate = delegate;
            this.unculledQuads = tintBambooQuads(delegate.getQuads(null));
            this.culledQuads = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                this.culledQuads.put(direction, tintBambooQuads(delegate.getQuads(direction)));
            }
        }

        static BlockModelPart wrap(BlockModelPart original) {
            return new BambooTintBlockModelPart(original);
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

        private static List<BakedQuad> tintBambooQuads(List<BakedQuad> quads) {
            if (quads.isEmpty()) {
                return quads;
            }

            List<BakedQuad> out = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                String spritePath = quad.sprite().contents().name().getPath().toLowerCase(java.util.Locale.ROOT);

                // The vertical stem models use bamboo_stalk. Every other quad on the living bamboo
                // block is foliage/sapling detail and should follow the regular autumn leaf palette.
                int tintIndex = spritePath.contains("bamboo_stalk") ? 1 : 0;

                out.add(new BakedQuad(
                        quad.vertices(),
                        tintIndex,
                        quad.direction(),
                        quad.sprite(),
                        quad.shade(),
                        quad.lightEmission(),
                        quad.hasAmbientOcclusion()
                ));
            }
            return List.copyOf(out);
        }
    }

    /** Preserves every model-part property and changes only otherwise-untinted quad tint indices. */
    private static final class TintForcingBlockModelPart implements BlockModelPart {
        private final BlockModelPart delegate;
        private final List<BakedQuad> unculledQuads;
        private final Map<Direction, List<BakedQuad>> culledQuads;

        private TintForcingBlockModelPart(BlockModelPart delegate, int tintIndex) {
            this.delegate = delegate;
            this.unculledQuads = forceTint(delegate.getQuads(null), tintIndex);
            this.culledQuads = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                this.culledQuads.put(direction, forceTint(delegate.getQuads(direction), tintIndex));
            }
        }

        static BlockModelPart wrapIfNeeded(BlockModelPart original, int tintIndex) {
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
            return new TintForcingBlockModelPart(original, tintIndex);
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

        private static List<BakedQuad> forceTint(List<BakedQuad> quads, int tintIndex) {
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
                            tintIndex,
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
