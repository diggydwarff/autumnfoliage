package dev.autumnfoliage.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional server policy. NeoForge SERVER configs are synced to clients that have the mod.
 * The server can enforce world zones and selected appearance settings, while explicitly
 * allowing clients to override chosen groups.
 */
public final class AutumnServerConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.EnumValue<Axis> AXIS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RANGES;

    private static final ModConfigSpec.DoubleValue LEAF_STRENGTH;
    private static final ModConfigSpec.DoubleValue FOLIAGE_VIBRANCY;
    private static final ModConfigSpec.DoubleValue FOLIAGE_BRIGHTNESS;
    private static final ModConfigSpec.DoubleValue SAPLING_STRENGTH;
    private static final ModConfigSpec.DoubleValue GRASS_STRENGTH;
    private static final ModConfigSpec.DoubleValue VINE_SHRUB_STRENGTH;
    private static final ModConfigSpec.DoubleValue EVERGREEN_STRENGTH;
    private static final ModConfigSpec.IntValue COLOR_PATCH_SIZE;
    private static final ModConfigSpec.BooleanValue AUTUMNAL_TROPICS;

    private static final ModConfigSpec.BooleanValue ALLOW_CLIENT_ENABLED_OVERRIDE;
    private static final ModConfigSpec.BooleanValue ALLOW_CLIENT_ZONE_OVERRIDE;
    private static final ModConfigSpec.BooleanValue ALLOW_CLIENT_TROPICAL_OVERRIDE;
    private static final ModConfigSpec.BooleanValue ALLOW_CLIENT_APPEARANCE_OVERRIDE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("zones");
        ENABLED = builder
                .comment(
                        "Master autumn switch for this world/server.",
                        "Only affects players that have Autumn Foliage installed.")
                .translation("autumnfoliage.config.server.enabled")
                .define("enabled", true);
        AXIS = builder
                .comment("World axis used for coordinate bands. Z is the usual north/south setup.")
                .translation("autumnfoliage.config.server.axis")
                .defineEnum("axis", Axis.Z);
        RANGES = builder
                .comment(
                        "Autumn-active coordinate ranges. Any number of ranges is supported.",
                        "Format: min,max,fadeDistance",
                        "An EMPTY list means the entire world is autumn-active.",
                        "When ranges are present, full autumn applies inside each range and fades smoothly outside its edges.",
                        "Overlapping ranges use the strongest effect rather than stacking.")
                .translation("autumnfoliage.config.server.ranges")
                .defineListAllowEmpty(
                        "ranges",
                        List.of(),
                        () -> "-18000,-9000,1500",
                        AutumnServerConfig::isRangeString
                );
        builder.pop();

        builder.push("appearance");
        LEAF_STRENGTH = builder
                .translation("autumnfoliage.config.server.leafStrength")
                .defineInRange("leafStrength", 1.0, 0.0, 1.0);
        FOLIAGE_VIBRANCY = builder
                .comment("Color saturation multiplier for autumn leaf-style foliage.")
                .translation("autumnfoliage.config.server.foliageVibrancy")
                .defineInRange("foliageVibrancy", 1.35, 0.50, 1.50);
        FOLIAGE_BRIGHTNESS = builder
                .comment("Brightness/luminance lift for autumn leaf-style foliage.")
                .translation("autumnfoliage.config.server.foliageBrightness")
                .defineInRange("foliageBrightness", 1.20, 0.50, 1.50);
        SAPLING_STRENGTH = builder
                .translation("autumnfoliage.config.server.saplingStrength")
                .defineInRange("saplingStrength", 0.9, 0.0, 1.0);
        GRASS_STRENGTH = builder
                .translation("autumnfoliage.config.server.grassStrength")
                .defineInRange("grassStrength", 0.46, 0.0, 1.0);
        VINE_SHRUB_STRENGTH = builder
                .translation("autumnfoliage.config.server.vineAndShrubStrength")
                .defineInRange("vineAndShrubStrength", 0.72, 0.0, 1.0);
        EVERGREEN_STRENGTH = builder
                .comment("Keeps obvious conifers mostly green while allowing a slight seasonal shift.")
                .translation("autumnfoliage.config.server.evergreenStrength")
                .defineInRange("evergreenStrength", 0.10, 0.0, 1.0);
        COLOR_PATCH_SIZE = builder
                .comment("Approximate horizontal size, in blocks, of smooth autumn color patches.")
                .translation("autumnfoliage.config.server.colorPatchSize")
                .defineInRange("colorPatchSize", 10, 2, 64);
        AUTUMNAL_TROPICS = builder
                .comment(
                        "Whether tropical/jungle vegetation should also become autumnal.",
                        "False keeps tropical biomes and explicitly tropical blocks lush/green.")
                .translation("autumnfoliage.config.server.autumnalTropics")
                .define("autumnalTropics", false);
        builder.pop();

        builder.push("clientOverrides");
        ALLOW_CLIENT_ENABLED_OVERRIDE = builder
                .comment("If true, each client may use its own local enabled/disabled value instead of the server value.")
                .translation("autumnfoliage.config.server.allowClientEnabledOverride")
                .define("allowClientEnabledOverride", false);
        ALLOW_CLIENT_ZONE_OVERRIDE = builder
                .comment("If true, each client may use its own axis and coordinate ranges instead of the server zones.")
                .translation("autumnfoliage.config.server.allowClientZoneOverride")
                .define("allowClientZoneOverride", false);
        ALLOW_CLIENT_TROPICAL_OVERRIDE = builder
                .comment("If true, each client may decide whether tropical/jungle vegetation becomes autumnal.")
                .translation("autumnfoliage.config.server.allowClientTropicalOverride")
                .define("allowClientTropicalOverride", false);
        ALLOW_CLIENT_APPEARANCE_OVERRIDE = builder
                .comment(
                        "If true, each client may use its own strength and color-patch settings.",
                        "Compatibility settings such as force-includes/excludes always remain client-side.")
                .translation("autumnfoliage.config.server.allowClientAppearanceOverride")
                .define("allowClientAppearanceOverride", true);
        builder.pop();

        SPEC = builder.build();
    }

    private AutumnServerConfig() {}

    public static boolean autumnalTropics() {
        return AUTUMNAL_TROPICS.get();
    }

    public static boolean allowClientEnabledOverride() {
        return ALLOW_CLIENT_ENABLED_OVERRIDE.get();
    }

    public static boolean allowClientZoneOverride() {
        return ALLOW_CLIENT_ZONE_OVERRIDE.get();
    }

    public static boolean allowClientTropicalOverride() {
        return ALLOW_CLIENT_TROPICAL_OVERRIDE.get();
    }

    public static boolean allowClientAppearanceOverride() {
        return ALLOW_CLIENT_APPEARANCE_OVERRIDE.get();
    }

    public static AppearanceSettings appearanceSnapshot() {
        return new AppearanceSettings(
                LEAF_STRENGTH.get(),
                FOLIAGE_VIBRANCY.get(),
                FOLIAGE_BRIGHTNESS.get(),
                SAPLING_STRENGTH.get(),
                GRASS_STRENGTH.get(),
                VINE_SHRUB_STRENGTH.get(),
                EVERGREEN_STRENGTH.get(),
                COLOR_PATCH_SIZE.get()
        );
    }

    public static ZoneSettings snapshot() {
        List<AutumnRange> parsed = new ArrayList<>();
        for (String raw : RANGES.get()) {
            AutumnConfig.parseRange(raw).ifPresent(parsed::add);
        }
        return new ZoneSettings(ENABLED.get(), AXIS.get(), List.copyOf(parsed));
    }

    private static boolean isRangeString(Object value) {
        return value instanceof String s && AutumnConfig.parseRange(s).isPresent();
    }
}
