package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import dev.autumnfoliage.config.RuntimeSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class AutumnColorizer {
    private AutumnColorizer() {}

    public static int color(BlockState state, BlockAndTintGetter level, BlockPos pos, int originalColor) {
        if (level == null || pos == null) {
            return originalColor;
        }

        double zoneStrength = ZoneStrength.at(pos);
        if (zoneStrength <= 0.0) {
            return originalColor;
        }

        VegetationType type = VegetationClassifier.classify(state);
        if (type == VegetationType.NONE) {
            return originalColor;
        }

        RuntimeSettings settings = AutumnConfig.runtime();
        if (!settings.autumnalTropics() &&
                (type == VegetationType.TROPICAL || VegetationClassifier.isTropicalBiome(level, pos))) {
            return originalColor;
        }
        double categoryStrength = switch (type) {
            case DECIDUOUS_LEAVES, TROPICAL -> settings.leafStrength();
            case EVERGREEN_LEAVES -> settings.evergreenStrength();
            case SAPLING -> settings.saplingStrength();
            case GRASS_FERN -> settings.grassStrength();
            case VINE_SHRUB -> settings.vineAndShrubStrength();
            default -> 0.0;
        };

        double finalStrength = zoneStrength * categoryStrength;
        if (finalStrength <= 0.0) {
            return originalColor;
        }

        // 1.21.1 expects ARGB. -1 is opaque white/no tint. Some older mod color handlers
        // still return 24-bit RGB, so normalize those to opaque before blending.
        int base = normalizeArgb(originalColor);
        int target = AutumnPalette.target(type, pos);
        return AutumnPalette.mix(base, target, finalStrength);
    }

    private static int normalizeArgb(int color) {
        if (color == -1) {
            return 0xFFFFFFFF;
        }
        if ((color & 0xFF000000) == 0) {
            return 0xFF000000 | (color & 0x00FFFFFF);
        }
        return color;
    }
}
