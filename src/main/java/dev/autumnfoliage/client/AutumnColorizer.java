package dev.autumnfoliage.client;

import dev.autumnfoliage.config.AutumnConfig;
import dev.autumnfoliage.config.RuntimeSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class AutumnColorizer {
    private AutumnColorizer() {}

    public static int color(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            int originalColor,
            int tintIndex
    ) {
        if (level == null || pos == null) {
            return originalColor;
        }
        return colorInternal(
                state,
                pos,
                originalColor,
                VegetationClassifier.isTropicalBiome(level, pos),
                tintIndex
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

    /**
     * Compatibility path for older Distant Horizons API implementations.
     * That API exposes the finished LOD color but not the separate untinted block-texture sample.
     * Convert the existing LOD color to neutral luminance before applying the autumn tint so the
     * original green biome tint does not muddy the result.
     */
    public static int colorForDistantHorizonsLegacy(
            BlockState state,
            BlockPos pos,
            int originalColor,
            String biomeSerial
    ) {
        if (state == null || pos == null) {
            return originalColor;
        }
        int neutralBase = neutralLuminanceBase(normalizeArgb(originalColor));
        return colorInternalDistantHorizons(
                state,
                pos,
                originalColor,
                neutralBase,
                VegetationClassifier.isTropicalBiomeSerial(biomeSerial)
        );
    }

    private static int colorInternal(
            BlockState state,
            BlockPos pos,
            int originalColor,
            boolean tropicalBiome,
            int tintIndex
    ) {
        VegetationType type = VegetationClassifier.classify(state);
        if (type == VegetationType.NONE) {
            return originalColor;
        }

        RuntimeSettings settings = AutumnConfig.runtime();
        if (!settings.autumnalTropics() &&
                (type == VegetationType.TROPICAL || tropicalBiome)) {
            return originalColor;
        }

        double zoneStrength = ZoneStrength.at(pos) * SeasonalTiming.strength(state, pos);
        if (zoneStrength <= 0.0) {
            return originalColor;
        }

        // Vanilla bamboo textures are pre-colored green, so the generic red/orange/gold leaf
        // palette is not reliable: its gold variants can multiply into a result that still looks
        // almost vanilla green. The model wrapper assigns every living bamboo quad to one of two
        // explicit channels. Both use dedicated smooth-only palettes so neighboring bamboo does not
        // jump across hard species-family boundaries.
        if (VegetationClassifier.isBambooPlant(state)) {
            double bambooStrength = zoneStrength * settings.leafStrength();
            if (bambooStrength <= 0.0) {
                return originalColor;
            }

            int bambooTarget = tintIndex == 1
                    ? AutumnPalette.bambooStalkTarget(pos)
                    : AutumnPalette.bambooLeafTarget(pos);
            double channelStrength = tintIndex == 1 ? bambooStrength * 0.90 : bambooStrength;
            return AutumnPalette.mix(0xFFFFFFFF, bambooTarget, channelStrength);
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

        // Minecraft 1.21.4 expects ARGB. -1 is opaque white/no tint. Some older mod color handlers
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
        VegetationType type = VegetationClassifier.classify(state);
        if (type == VegetationType.NONE) {
            return originalColor;
        }

        RuntimeSettings settings = AutumnConfig.runtime();
        if (!settings.autumnalTropics() &&
                (type == VegetationType.TROPICAL || tropicalBiome)) {
            return originalColor;
        }

        double zoneStrength = ZoneStrength.at(pos) * SeasonalTiming.strength(state, pos);
        if (zoneStrength <= 0.0) {
            return originalColor;
        }

        // DH stores one representative color per block sample rather than separate model quads,
        // so it cannot reproduce the near renderer's independent bamboo leaf/stalk channels. Use a
        // dedicated composite bamboo tint that blends both channels instead of falling back to the
        // generic tropical palette. This keeps distant bamboo visually aligned with nearby bamboo.
        if (VegetationClassifier.isBambooPlant(state)) {
            double finalStrength = zoneStrength * settings.leafStrength();
            if (finalStrength <= 0.0) {
                return originalColor;
            }
            int targetTint = AutumnPalette.bambooLodTarget(pos);
            int targetRenderedColor = multiplyArgb(normalizeArgb(baseColor), targetTint);
            return AutumnPalette.mix(normalizeArgb(originalColor), targetRenderedColor, finalStrength);
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

    private static int neutralLuminanceBase(int color) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;

        // Rec. 709 luminance. A small lift compensates for the fact that DH's finished color may
        // already include biome tint and shading. Keeping this neutral preserves LOD texture/light
        // variation without carrying the original green hue into the autumn palette.
        int luma = (int) Math.round(0.2126 * r + 0.7152 * g + 0.0722 * b);
        int neutral = Math.max(0, Math.min(255, (int) Math.round(luma * 1.12)));
        return (a << 24) | (neutral << 16) | (neutral << 8) | neutral;
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
