package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import net.minecraft.core.BlockPos;

/**
 * Bright autumn palette based on the supplied Autumnpack 3.0 reference. The alpha.2 palette
 * intentionally favors saturated red, orange, amber and gold over brown/rust so dense forests
 * still read as colorful autumn rather than dead foliage.
 * Colors are opaque ARGB, matching the 1.21.1 BlockColor contract.
 */
public final class AutumnPalette {
    private static final int[] LEAF_GRADIENT = {
            0xFFB52328,
            0xFFCB3020,
            0xFFDE461B,
            0xFFE95F16,
            0xFFEF7D16,
            0xFFF09A19,
            0xFFF1B321,
            0xFFEFC63A,
            0xFFE9D151
    };

    private static final int[] GRASS_GRADIENT = {
            0xFFA09A35,
            0xFFB2A63B,
            0xFFC0B345,
            0xFFC49A34,
            0xFFB78128
    };

    private static final int EVERGREEN_AUTUMN = 0xFF7F8E48;

    private AutumnPalette() {}

    public static int target(VegetationType type, BlockPos pos) {
        return switch (type) {
            case GRASS_FERN -> sampleGradient(GRASS_GRADIENT, smoothNoise(pos, 0x51A77E21L));
            case EVERGREEN_LEAVES -> EVERGREEN_AUTUMN;
            case DECIDUOUS_LEAVES, TROPICAL, SAPLING, VINE_SHRUB ->
                    sampleGradient(LEAF_GRADIENT, smoothNoise(pos, 0xA17A5EEDL));
            default -> 0xFFFFFFFF;
        };
    }

    private static double smoothNoise(BlockPos pos, long salt) {
        int scale = Math.max(2, AutumnConfig.runtime().colorPatchSize());
        double x = pos.getX() / (double) scale;
        double z = pos.getZ() / (double) scale;

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
}
