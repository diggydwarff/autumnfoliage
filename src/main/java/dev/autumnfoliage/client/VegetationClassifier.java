package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import dev.autumnfoliage.config.RuntimeSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Broad, deliberately conservative vegetation detection.
 *
 * Results are cached per block because this code is hit heavily during chunk rebuilds.
 * Config reloads clear the cache through {@link #clearCache()}.
 */
public final class VegetationClassifier {
    private static final ConcurrentMap<Block, VegetationType> CACHE = new ConcurrentHashMap<>();

    private VegetationClassifier() {}

    public static VegetationType classify(BlockState state) {
        return CACHE.computeIfAbsent(state.getBlock(), block -> classifyUncached(state));
    }

    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Whether it is reasonably safe to assign tint index 0 to every otherwise-untinted quad
     * in this block model. Ground blocks such as grass blocks are intentionally excluded so
     * their dirt/soil faces are not recolored.
     */
    public static boolean canForceTintWholeModel(BlockState state, VegetationType type) {
        if (type == VegetationType.NONE || type == VegetationType.TROPICAL) {
            return false;
        }

        if (type != VegetationType.GRASS_FERN) {
            return true;
        }

        String path = blockPath(state);
        return !(path.contains("grass_block") ||
                path.endsWith("_turf") ||
                path.startsWith("turf_") ||
                path.contains("sod_block") ||
                path.endsWith("_sod"));
    }

    private static VegetationType classifyUncached(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) {
            return VegetationType.NONE;
        }

        String fullId = id.toString().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        RuntimeSettings settings = AutumnConfig.runtime();

        if (settings.forceExclude().contains(fullId)) {
            return VegetationType.NONE;
        }

        boolean forced = settings.forceInclude().contains(fullId);
        boolean tropical = containsAny(path, settings.tropicalKeywords());
        boolean evergreen = containsAny(path, settings.evergreenKeywords());

        // Leaves are checked before generic exclusions so names such as
        // "flowering_azalea_leaves" are not accidentally discarded as flowers.
        if (isLeaves(state, block, path)) {
            if (!forced && tropical) {
                return VegetationType.TROPICAL;
            }
            if (!forced && evergreen) {
                return VegetationType.EVERGREEN_LEAVES;
            }
            return VegetationType.DECIDUOUS_LEAVES;
        }

        if (forced) {
            return VegetationType.DECIDUOUS_LEAVES;
        }

        if (isHardExcluded(path)) {
            return VegetationType.NONE;
        }

        if (block instanceof SaplingBlock || path.contains("sapling")) {
            if (tropical) {
                return VegetationType.TROPICAL;
            }
            if (evergreen) {
                return VegetationType.EVERGREEN_LEAVES;
            }
            return VegetationType.SAPLING;
        }

        if (path.contains("grass") || path.contains("fern")) {
            return tropical ? VegetationType.TROPICAL : VegetationType.GRASS_FERN;
        }

        if (path.contains("vine") || path.contains("shrub") || path.contains("bush") ||
                path.contains("hedge") || path.contains("bramble") || path.contains("groundcover")) {
            return tropical ? VegetationType.TROPICAL : VegetationType.VINE_SHRUB;
        }

        return VegetationType.NONE;
    }

    private static boolean isLeaves(BlockState state, Block block, String path) {
        return block instanceof LeavesBlock ||
                state.is(BlockTags.LEAVES) ||
                path.contains("leaves") ||
                path.endsWith("_leaf") ||
                path.startsWith("leaf_") ||
                path.contains("foliage") ||
                path.contains("needles") ||
                path.contains("needle_leaves") ||
                path.contains("frond");
    }

    private static boolean isHardExcluded(String path) {
        return path.contains("cactus") ||
                path.contains("crop") ||
                path.contains("wheat") ||
                path.contains("carrot") ||
                path.contains("potato") ||
                path.contains("beetroot") ||
                path.contains("nether_wart") ||
                path.contains("cocoa") ||
                path.contains("sugar_cane") ||
                path.contains("bamboo") ||
                path.contains("mushroom") ||
                path.contains("fungus") ||
                path.contains("coral") ||
                path.contains("seagrass") ||
                path.contains("kelp") ||
                path.contains("flower") ||
                path.contains("torchflower") ||
                path.contains("pitcher") ||
                path.contains("melon") ||
                path.contains("pumpkin");
    }

    private static String blockPath(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String path, Iterable<String> keywords) {
        for (String keyword : keywords) {
            if (!keyword.isBlank() && path.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
