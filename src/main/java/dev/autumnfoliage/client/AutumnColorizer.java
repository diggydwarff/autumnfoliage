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
        return colorInternal(
                state,
                pos,
                originalColor,
                VegetationClassifier.isTropicalBiome(level, pos)
        );
    }

    /**
     * Color path used by Distant Horizons. DH may be resolving LODs far outside Minecraft's
     * currently loaded chunks, so tropical-biome detection comes from DH's biome wrapper rather
     * than BlockAndTintGetter/ClientLevel.
     */
    public static int colorForDistantHorizons(
            BlockState state,
            BlockPos pos,
            int originalColor,
            int baseColor,
            String biomeSerial
    ) {
        if (state == null || pos == null) {
            return originalColor;
        }
        return colorInternalDistantHorizons(
                state,
                pos,
                originalColor,
                baseColor,
                VegetationClassifier.isTropicalBiomeSerial(biomeSerial)
        );
    }

    private static int colorInternal(
            BlockState state,
            BlockPos pos,
            int originalColor,
            boolean tropicalBiome
    ) {
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
                (type == VegetationType.TROPICAL || tropicalBiome)) {
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
        int target = AutumnPalette.target(type, state, pos);
        if (usesLeafPalette(type)) {
            target = AutumnPalette.adjustFoliageAppearance(
                    target,
                    settings.foliageVibrancy(),
                    settings.foliageBrightness()
            );
        }

        // Normal Minecraft/Sodium rendering expects a tint multiplier. The block texture and world
        // lighting are applied later by the renderer, so return the autumn tint itself here.
        return AutumnPalette.mix(base, target, finalStrength);
    }

    private static int colorInternalDistantHorizons(
            BlockState state,
            BlockPos pos,
            int originalColor,
            int baseColor,
            boolean tropicalBiome
    ) {
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
                (type == VegetationType.TROPICAL || tropicalBiome)) {
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

        int targetTint = AutumnPalette.target(type, state, pos);
        if (usesLeafPalette(type)) {
            targetTint = AutumnPalette.adjustFoliageAppearance(
                    targetTint,
                    settings.foliageVibrancy(),
                    settings.foliageBrightness()
            );
        }

        // DH exposes both its finished LOD color and the untinted base color it sampled from the
        // block texture. Re-apply our autumn tint to that base color instead of replacing the entire
        // LOD sample with a flat bright palette color. This mirrors the vanilla/Sodium pipeline:
        //     final leaf color ~= texture/base color * tint color
        // and removes the obvious bright DH -> dark vanilla handoff without throwing away the
        // stronger autumn palette.
        int targetRenderedColor = multiplyArgb(normalizeArgb(baseColor), targetTint);
        return AutumnPalette.mix(normalizeArgb(originalColor), targetRenderedColor, finalStrength);
    }

    private static boolean usesLeafPalette(VegetationType type) {
        return switch (type) {
            case DECIDUOUS_LEAVES, TROPICAL, SAPLING, VINE_SHRUB -> true;
            default -> false;
        };
    }

    private static int multiplyArgb(int base, int tint) {
        int ba = (base >>> 24) & 0xFF;
        int br = (base >>> 16) & 0xFF;
        int bg = (base >>> 8) & 0xFF;
        int bb = base & 0xFF;
        int ta = (tint >>> 24) & 0xFF;
        int tr = (tint >>> 16) & 0xFF;
        int tg = (tint >>> 8) & 0xFF;
        int tb = tint & 0xFF;

        int a = ba * ta / 255;
        int r = br * tr / 255;
        int g = bg * tg / 255;
        int b = bb * tb / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
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
