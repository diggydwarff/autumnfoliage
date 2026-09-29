package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * High-chroma autumn palette inspired by the supplied Autumnpack 3.0 reference.
 *
 * The reference pack gets a lot of its forest variety from species-specific textures: some trees
 * are red, some orange, and some gold. A single continuous red-to-yellow noise gradient tended to
 * collapse toward muddy middle colors, so Autumn Foliage mirrors that idea by assigning each leaf
 * species to a stable color family, then varying that family in deterministic world-space patches.
 */
public final class AutumnPalette {
    private static final int[] RED_FAMILY = {
            0xFFFF2418,
            0xFFFF3218,
            0xFFFF3F1C,
            0xFFFF4B21,
            0xFFF51F2D,
            0xFFFF5A2A
    };

    private static final int[] ORANGE_FAMILY = {
            0xFFFF6500,
            0xFFFF7400,
            0xFFFF8200,
            0xFFFF9000,
            0xFFFFA000,
            0xFFFFB000
    };

    private static final int[] GOLD_FAMILY = {
            0xFFFFB800,
            0xFFFFC400,
            0xFFFFD000,
            0xFFFFDC00,
            0xFFFFE515,
            0xFFFFEF3A
    };

    private static final int[] GRASS_GRADIENT = {
            0xFFA5A63D,
            0xFFB9AE3E,
            0xFFCDB94A,
            0xFFD2A13B,
            0xFFC58A2E
    };

    private static final int EVERGREEN_AUTUMN = 0xFF84934A;

    private AutumnPalette() {}

    public static int target(VegetationType type, BlockState state, BlockPos pos) {
        return switch (type) {
            case GRASS_FERN -> sampleGradient(GRASS_GRADIENT, smoothNoise(pos, 0x51A77E21L));
            case EVERGREEN_LEAVES -> EVERGREEN_AUTUMN;
            case DECIDUOUS_LEAVES, TROPICAL, SAPLING, VINE_SHRUB -> leafTarget(state, pos);
            default -> 0xFFFFFFFF;
        };
    }

    private static int leafTarget(BlockState state, BlockPos pos) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        long speciesSalt = stableHash64(id == null ? "unknown" : id.toString());

        Family dominantFamily = familyFor(path, speciesSalt);

        // Keep the dominant family coherent across an entire canopy. Earlier versions selected a
        // family from small square world cells; a large crown crossing a cell edge could therefore
        // become visibly half red and half orange. Use much larger jittered Voronoi-style
        // "tree neighborhoods" for family selection instead. Their irregular borders avoid obvious
        // north/south or east/west seams, while the smaller smooth shade noise below still gives a
        // canopy natural internal variation.
        int patchSize = Math.max(2, AutumnConfig.runtime().colorPatchSize());
        int familyRegionSize = Math.max(24, patchSize * 3);
        RegionKey region = nearestTreeRegion(pos, familyRegionSize, speciesSalt);

        double familySelector = hash01(region.x(), region.z(), speciesSalt ^ 0x6F17A11DL);
        Family family = variedFamily(dominantFamily, familySelector);
        int[] palette = switch (family) {
            case RED -> RED_FAMILY;
            case ORANGE -> ORANGE_FAMILY;
            case GOLD -> GOLD_FAMILY;
        };

        // Shade changes are smooth and remain inside the selected family. This gives individual
        // trees texture without introducing hard block-grid color splits through the canopy.
        double shadeSelector = smoothNoise(pos, speciesSalt ^ 0xA17A5EEDL, Math.max(6, patchSize));
        return sampleGradient(palette, shadeSelector);
    }


    private static Family variedFamily(Family dominant, double selector) {
        return switch (dominant) {
            case RED -> selector < 0.60 ? Family.RED : selector < 0.86 ? Family.ORANGE : Family.GOLD;
            case ORANGE -> selector < 0.20 ? Family.RED : selector < 0.78 ? Family.ORANGE : Family.GOLD;
            case GOLD -> selector < 0.12 ? Family.RED : selector < 0.36 ? Family.ORANGE : Family.GOLD;
        };
    }

    private static Family familyFor(String path, long speciesSalt) {
        if (containsAny(path,
                "birch", "aspen", "poplar", "ginkgo", "linden", "basswood", "willow", "cottonwood")) {
            return Family.GOLD;
        }
        if (containsAny(path,
                "maple", "acacia", "dark_oak", "cherry", "dogwood", "sweetgum", "tupelo", "sumac")) {
            return Family.RED;
        }
        if (containsAny(path,
                "oak", "beech", "elm", "chestnut", "sycamore", "hornbeam", "hickory", "walnut")) {
            return Family.ORANGE;
        }

        // Unknown modded species still get a stable family rather than all drifting toward the same
        // generic color. This is deliberately deterministic across sessions and multiplayer clients.
        return switch ((int) Math.floorMod(speciesSalt, 3L)) {
            case 0 -> Family.RED;
            case 1 -> Family.ORANGE;
            default -> Family.GOLD;
        };
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static double smoothNoise(BlockPos pos, long salt) {
        return smoothNoise(pos, salt, Math.max(2, AutumnConfig.runtime().colorPatchSize()));
    }

    private static double smoothNoise(BlockPos pos, long salt, int scale) {
        int safeScale = Math.max(2, scale);
        double x = pos.getX() / (double) safeScale;
        double z = pos.getZ() / (double) safeScale;

        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double tx = smoothstep(x - x0);
        double tz = smoothstep(z - z0);

        double n00 = hash01(x0, z0, salt);
        double n10 = hash01(x1, z0, salt);
        double n01 = hash01(x0, z1, salt);
        double n11 = hash01(x1, z1, salt);

        double nx0 = lerp(n00, n10, tx);
        double nx1 = lerp(n01, n11, tx);
        return lerp(nx0, nx1, tz);
    }

    /**
     * Returns the nearest jittered region seed around the supplied position. This is a cheap
     * two-dimensional Voronoi partition: neighboring regions have irregular borders instead of
     * square grid lines, which is much less noticeable when a forest canopy crosses a boundary.
     */
    private static RegionKey nearestTreeRegion(BlockPos pos, int scale, long salt) {
        double gx = pos.getX() / (double) scale;
        double gz = pos.getZ() / (double) scale;
        int baseX = fastFloor(gx);
        int baseZ = fastFloor(gz);

        int bestX = baseX;
        int bestZ = baseZ;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cellX = baseX + dx;
                int cellZ = baseZ + dz;
                double jitterX = 0.15 + hash01(cellX, cellZ, salt ^ 0x31D0A55EL) * 0.70;
                double jitterZ = 0.15 + hash01(cellX, cellZ, salt ^ 0x58C3E91BL) * 0.70;
                double pointX = cellX + jitterX;
                double pointZ = cellZ + jitterZ;
                double ddx = gx - pointX;
                double ddz = gz - pointZ;
                double distance = ddx * ddx + ddz * ddz;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = cellX;
                    bestZ = cellZ;
                }
            }
        }

        return new RegionKey(bestX, bestZ);
    }

    private static int sampleGradient(int[] colors, double value) {
        if (colors.length == 1) return colors[0];
        double scaled = clamp01(value) * (colors.length - 1);
        int index = Math.min(colors.length - 2, (int) Math.floor(scaled));
        double t = scaled - index;
        return mix(colors[index], colors[index + 1], t);
    }

    /** Mix two ARGB colors. */
    public static int mix(int from, int to, double t) {
        double x = clamp01(t);
        int fa = (from >>> 24) & 0xFF;
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;

        int a = (int) Math.round(lerp(fa, ta, x));
        int r = (int) Math.round(lerp(fr, tr, x));
        int g = (int) Math.round(lerp(fg, tg, x));
        int b = (int) Math.round(lerp(fb, tb, x));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Applies user-facing saturation and brightness controls to an autumn target. Brightness is a
     * gentle highlight lift rather than raw RGB multiplication, which is useful because Minecraft
     * subsequently multiplies this tint by the leaf texture itself.
     */
    public static int adjustFoliageAppearance(int color, double vibrancy, double brightness) {
        double b = Math.max(0.50, Math.min(1.50, brightness));
        int brightened = adjustBrightness(color, b);
        return adjustVibrancy(brightened, vibrancy);
    }

    /**
     * Lifts RGB midtones with a gamma curve instead of mixing toward white. That keeps oranges and
     * yellows colorful while making the tint survive Minecraft's later multiplication by the real
     * (often fairly dark) leaf texture.
     */
    private static int adjustBrightness(int color, double brightness) {
        int a = (color >>> 24) & 0xFF;
        double r = ((color >>> 16) & 0xFF) / 255.0;
        double g = ((color >>> 8) & 0xFF) / 255.0;
        double b = (color & 0xFF) / 255.0;

        if (brightness < 1.0) {
            r *= brightness;
            g *= brightness;
            b *= brightness;
        } else if (brightness > 1.0) {
            double gamma = 1.0 / brightness;
            r = Math.pow(r, gamma);
            g = Math.pow(g, gamma);
            b = Math.pow(b, gamma);
        }

        return (a << 24) | (toByte(r) << 16) | (toByte(g) << 8) | toByte(b);
    }

    /** Scales HSV saturation while keeping hue and value stable. */
    public static int adjustVibrancy(int color, double vibrancy) {
        int a = (color >>> 24) & 0xFF;
        double r = ((color >>> 16) & 0xFF) / 255.0;
        double g = ((color >>> 8) & 0xFF) / 255.0;
        double b = (color & 0xFF) / 255.0;

        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;
        if (delta <= 1.0e-9 || max <= 1.0e-9) {
            return color;
        }

        double saturation = delta / max;
        double targetSaturation = clamp01(saturation * Math.max(0.0, vibrancy));
        double scale = targetSaturation / saturation;

        r = max - (max - r) * scale;
        g = max - (max - g) * scale;
        b = max - (max - b) * scale;

        return (a << 24)
                | (toByte(r) << 16)
                | (toByte(g) << 8)
                | toByte(b);
    }

    private static int toByte(double normalized) {
        return clampByte(clamp01(normalized) * 255.0);
    }

    private static int clampByte(double value) {
        return (int) Math.round(Math.max(0.0, Math.min(255.0, value)));
    }

    private static double hash01(int x, int z, long salt) {
        long h = salt;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 27);
        h ^= z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    private static long stableHash64(String value) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < value.length(); i++) {
            h ^= value.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    private static int fastFloor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static double smoothstep(double x) {
        double v = clamp01(x);
        return v * v * (3.0 - 2.0 * v);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record RegionKey(int x, int z) {}

    private enum Family {
        RED,
        ORANGE,
        GOLD
    }
}
