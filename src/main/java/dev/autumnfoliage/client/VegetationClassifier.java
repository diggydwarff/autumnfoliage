package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import dev.autumnfoliage.config.RuntimeSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
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
     * Biome-level tropical detection catches generic vegetation (especially vines and grass)
     * growing inside jungle/rainforest biomes even when the block id itself is generic.
     *
     * BlockAndTintGetter deliberately does not expose getBiome in 1.21.8. Vanilla chunk meshing
     * commonly passes RenderChunkRegion here, so we use a LevelReader when available and otherwise
     * fall back to the active ClientLevel. The fallback is read-only and points at the same loaded
     * world currently being meshed.
     */
    public static boolean isTropicalBiome(BlockAndTintGetter level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        Holder<Biome> biome = null;
        if (level instanceof LevelReader reader) {
            biome = reader.getBiome(pos);
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel clientLevel = minecraft == null ? null : minecraft.level;
            if (clientLevel != null) {
                biome = clientLevel.getBiome(pos);
            }
        }

        if (biome == null) {
            return false;
        }

        String path = biome.unwrapKey()
                .map(key -> key.location().getPath().toLowerCase(Locale.ROOT))
                .orElse("");
        return containsAny(path, AutumnConfig.runtime().tropicalBiomeKeywords());
    }

    /**
     * Distant Horizons can resolve LOD colors for positions that are outside Minecraft's loaded
     * client chunks. In that case its biome wrapper serial string is the reliable biome source.
     */
    public static boolean isTropicalBiomeSerial(String biomeSerial) {
        if (biomeSerial == null || biomeSerial.isBlank()) {
            return false;
        }
        return containsAny(
                biomeSerial.toLowerCase(Locale.ROOT),
                AutumnConfig.runtime().tropicalBiomeKeywords()
        );
    }

    /**
     * Whether it is reasonably safe to assign tint index 0 to every otherwise-untinted quad
     * in this block model. Ground blocks such as grass blocks are intentionally excluded so
     * their dirt/soil faces are not recolored.
     */
    public static boolean canForceTintWholeModel(BlockState state, VegetationType type) {
        if (type == VegetationType.NONE) {
            return false;
        }
        if (type == VegetationType.TROPICAL && !AutumnConfig.runtime().autumnalTropics()) {
            return false;
        }

        String path = blockPath(state);

        // Bamboo is a multipart model: its stalk quads are intentionally untinted while the
        // small/large leaf models already carry tint index 0. Never force-tint the whole model,
        // otherwise opting bamboo into autumn processing would recolor the stalk as well.
        if (isBambooPlant(path)) {
            return false;
        }

        if (type != VegetationType.GRASS_FERN) {
            return true;
        }

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

        // Vanilla bamboo is unusual: the block id represents both the untinted stalk and
        // tint-indexed leaf model parts. Keep it excluded by default, but allow the user's
        // Tropical Block Keywords or Force Include settings to opt the actual plant in.
        // Bamboo building blocks (planks, fences, doors, etc.) still remain excluded.
        if (isBambooPlant(path)) {
            if (forced) {
                return VegetationType.DECIDUOUS_LEAVES;
            }
            if (tropical) {
                return VegetationType.TROPICAL;
            }
            return VegetationType.NONE;
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
                path.contains("hedge") || path.contains("bramble") || path.contains("groundcover") ||
                path.contains("liana") || path.contains("rattan")) {
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

    static boolean isBambooPlant(BlockState state) {
        return isBambooPlant(blockPath(state));
    }

    private static boolean isBambooPlant(String path) {
        return path.equals("bamboo") || path.equals("bamboo_sapling");
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
