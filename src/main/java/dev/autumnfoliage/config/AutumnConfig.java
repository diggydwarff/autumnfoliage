package dev.autumnfoliage.config;

import dev.autumnfoliage.network.ServerZoneOverride;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Client preferences plus local fallback values used when no server policy is active or overrides are allowed. */
public final class AutumnConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.EnumValue<Axis> AXIS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RANGES;
    private static final ModConfigSpec.BooleanValue FORCE_TINT_UNTINTED;

    private static final ModConfigSpec.DoubleValue LEAF_STRENGTH;
    private static final ModConfigSpec.DoubleValue FOLIAGE_VIBRANCY;
    private static final ModConfigSpec.DoubleValue FOLIAGE_BRIGHTNESS;
    private static final ModConfigSpec.DoubleValue SAPLING_STRENGTH;
    private static final ModConfigSpec.DoubleValue GRASS_STRENGTH;
    private static final ModConfigSpec.DoubleValue VINE_SHRUB_STRENGTH;
    private static final ModConfigSpec.DoubleValue EVERGREEN_STRENGTH;
    private static final ModConfigSpec.BooleanValue AUTUMNAL_TROPICS;
    private static final ModConfigSpec.IntValue COLOR_PATCH_SIZE;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_INCLUDE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_EXCLUDE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EVERGREEN_KEYWORDS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TROPICAL_KEYWORDS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TROPICAL_BIOME_KEYWORDS;

    private static volatile RuntimeSettings runtime = RuntimeSettings.defaults();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("zones");
        ENABLED = builder
                .comment(
                        "Local autumn switch.",
                        "Used directly on unmodded servers. On modded servers it is used only if the server allows client override.")
                .translation("autumnfoliage.config.client.enabled")
                .define("enabled", true);
        AXIS = builder
                .comment("World axis used for coordinate bands. Z is the usual north/south setup.")
                .translation("autumnfoliage.config.client.axis")
                .defineEnum("axis", Axis.Z);
        RANGES = builder
                .comment(
                        "Local autumn coordinate ranges. Any number is supported.",
                        "Format: min,max,fadeDistance",
                        "An EMPTY list means the entire world is autumn-active.",
                        "When ranges are present, full autumn applies inside each range and fades smoothly outside its edges.")
                .translation("autumnfoliage.config.client.ranges")
                .defineListAllowEmpty(
                        "ranges",
                        List.of(),
                        () -> "-18000,-9000,1500",
                        AutumnConfig::isRangeString
                );
        builder.pop();

        builder.push("rendering");
        FORCE_TINT_UNTINTED = builder
                .comment(
                        "Adds a tint index to otherwise untinted vegetation model quads so more modded plants can be recolored.",
                        "Disable only if a particular rendering mod/model behaves badly.")
                .translation("autumnfoliage.config.client.forceTintUntintedModels")
                .define("forceTintUntintedModels", true);
        LEAF_STRENGTH = builder
                .translation("autumnfoliage.config.client.leafStrength")
                .defineInRange("leafStrength", 1.0, 0.0, 1.0);
        FOLIAGE_VIBRANCY = builder
                .comment(
                        "Color saturation multiplier for autumn leaf-style foliage.",
                        "1.0 is the base palette; values above 1.0 produce richer reds/oranges/golds.")
                .translation("autumnfoliage.config.client.foliageVibrancy")
                .defineInRange("foliageVibrancy", 1.35, 0.50, 1.50);
        FOLIAGE_BRIGHTNESS = builder
                .comment(
                        "Brightness/luminance lift for autumn leaf-style foliage.",
                        "This is especially useful for darker modded leaf textures, which multiply the autumn tint into their texture shading.")
                .translation("autumnfoliage.config.client.foliageBrightness")
                .defineInRange("foliageBrightness", 1.20, 0.50, 1.50);
        SAPLING_STRENGTH = builder
                .translation("autumnfoliage.config.client.saplingStrength")
                .defineInRange("saplingStrength", 0.9, 0.0, 1.0);
        GRASS_STRENGTH = builder
                .translation("autumnfoliage.config.client.grassStrength")
                .defineInRange("grassStrength", 0.46, 0.0, 1.0);
        VINE_SHRUB_STRENGTH = builder
                .translation("autumnfoliage.config.client.vineAndShrubStrength")
                .defineInRange("vineAndShrubStrength", 0.72, 0.0, 1.0);
        EVERGREEN_STRENGTH = builder
                .comment("Keeps obvious conifers mostly green while still allowing a slight seasonal shift.")
                .translation("autumnfoliage.config.client.evergreenStrength")
                .defineInRange("evergreenStrength", 0.10, 0.0, 1.0);
        AUTUMNAL_TROPICS = builder
                .comment(
                        "Whether tropical/jungle vegetation should receive the autumn effect.",
                        "False keeps tropical biomes and explicitly tropical blocks lush/green.",
                        "On a modded server this local value is used only if the server allows tropical override.")
                .translation("autumnfoliage.config.client.autumnalTropics")
                .define("autumnalTropics", false);
        COLOR_PATCH_SIZE = builder
                .comment("Approximate horizontal size, in blocks, of smooth color patches. Larger values make whole trees/stands more consistent.")
                .translation("autumnfoliage.config.client.colorPatchSize")
                .defineInRange("colorPatchSize", 10, 2, 64);
        builder.pop();

        builder.push("compatibility");
        FORCE_INCLUDE = builder
                .comment("Exact block ids to force into autumn processing, e.g. \"somemod:odd_tree_foliage\".")
                .translation("autumnfoliage.config.client.forceInclude")
                .defineListAllowEmpty("forceInclude", List.of(), () -> "modid:block", AutumnConfig::isResourceIdString);
        FORCE_EXCLUDE = builder
                .comment("Exact block ids that must never be recolored.")
                .translation("autumnfoliage.config.client.forceExclude")
                .defineListAllowEmpty("forceExclude", List.of(), () -> "modid:block", AutumnConfig::isResourceIdString);
        EVERGREEN_KEYWORDS = builder
                .comment("Path fragments that identify evergreen/conifer foliage across mods.")
                .translation("autumnfoliage.config.client.evergreenKeywords")
                .defineListAllowEmpty(
                        "evergreenKeywords",
                        List.of("spruce", "pine", "fir", "cedar", "redwood", "sequoia", "cypress", "juniper", "hemlock", "conifer", "evergreen"),
                        () -> "pine",
                        AutumnConfig::isSimpleString
                );
        TROPICAL_KEYWORDS = builder
                .comment("Path fragments that identify explicitly tropical vegetation.")
                .translation("autumnfoliage.config.client.tropicalKeywords")
                .defineListAllowEmpty(
                        "tropicalKeywords",
                        List.of("jungle", "palm", "coconut", "banana", "tropical", "rainforest", "mangrove", "monsoon"),
                        () -> "palm",
                        AutumnConfig::isSimpleString
                );
        TROPICAL_BIOME_KEYWORDS = builder
                .comment(
                        "Biome id path fragments treated as tropical when autumnalTropics is false.",
                        "This catches generic vines/grass inside jungle-like biomes even when the block itself has no tropical name.")
                .translation("autumnfoliage.config.client.tropicalBiomeKeywords")
                .defineListAllowEmpty(
                        "tropicalBiomeKeywords",
                        List.of("jungle", "rainforest", "tropical", "tropics", "monsoon", "mangrove"),
                        () -> "rainforest",
                        AutumnConfig::isSimpleString
                );
        builder.pop();

        SPEC = builder.build();
    }

    private AutumnConfig() {}

    public static RuntimeSettings runtime() {
        return runtime;
    }

    public static ClientConfigSnapshot clientSnapshot() {
        return new ClientConfigSnapshot(
                ENABLED.get(),
                AXIS.get(),
                localZoneSnapshot().ranges(),
                FORCE_TINT_UNTINTED.get(),
                LEAF_STRENGTH.get(),
                FOLIAGE_VIBRANCY.get(),
                FOLIAGE_BRIGHTNESS.get(),
                SAPLING_STRENGTH.get(),
                GRASS_STRENGTH.get(),
                VINE_SHRUB_STRENGTH.get(),
                EVERGREEN_STRENGTH.get(),
                AUTUMNAL_TROPICS.get(),
                COLOR_PATCH_SIZE.get(),
                copyStrings(FORCE_INCLUDE.get()),
                copyStrings(FORCE_EXCLUDE.get()),
                copyStrings(EVERGREEN_KEYWORDS.get()),
                copyStrings(TROPICAL_KEYWORDS.get()),
                copyStrings(TROPICAL_BIOME_KEYWORDS.get())
        );
    }

    public static ClientConfigSnapshot defaultSnapshot() {
        List<AutumnRange> defaultRanges = new ArrayList<>();
        for (String raw : copyStrings(RANGES.getDefault())) {
            parseRange(raw).ifPresent(defaultRanges::add);
        }
        return new ClientConfigSnapshot(
                ENABLED.getDefault(),
                AXIS.getDefault(),
                List.copyOf(defaultRanges),
                FORCE_TINT_UNTINTED.getDefault(),
                LEAF_STRENGTH.getDefault(),
                FOLIAGE_VIBRANCY.getDefault(),
                FOLIAGE_BRIGHTNESS.getDefault(),
                SAPLING_STRENGTH.getDefault(),
                GRASS_STRENGTH.getDefault(),
                VINE_SHRUB_STRENGTH.getDefault(),
                EVERGREEN_STRENGTH.getDefault(),
                AUTUMNAL_TROPICS.getDefault(),
                COLOR_PATCH_SIZE.getDefault(),
                copyStrings(FORCE_INCLUDE.getDefault()),
                copyStrings(FORCE_EXCLUDE.getDefault()),
                copyStrings(EVERGREEN_KEYWORDS.getDefault()),
                copyStrings(TROPICAL_KEYWORDS.getDefault()),
                copyStrings(TROPICAL_BIOME_KEYWORDS.getDefault())
        );
    }

    public static void applyClientSnapshot(ClientConfigSnapshot snapshot) {
        ENABLED.set(snapshot.enabled());
        AXIS.set(snapshot.axis());
        RANGES.set(snapshot.ranges().stream()
                .map(r -> r.min() + "," + r.max() + "," + r.fadeDistance())
                .toList());
        FORCE_TINT_UNTINTED.set(snapshot.forceTintUntintedModels());
        LEAF_STRENGTH.set(snapshot.leafStrength());
        FOLIAGE_VIBRANCY.set(snapshot.foliageVibrancy());
        FOLIAGE_BRIGHTNESS.set(snapshot.foliageBrightness());
        SAPLING_STRENGTH.set(snapshot.saplingStrength());
        GRASS_STRENGTH.set(snapshot.grassStrength());
        VINE_SHRUB_STRENGTH.set(snapshot.vineAndShrubStrength());
        EVERGREEN_STRENGTH.set(snapshot.evergreenStrength());
        AUTUMNAL_TROPICS.set(snapshot.autumnalTropics());
        COLOR_PATCH_SIZE.set(snapshot.colorPatchSize());
        FORCE_INCLUDE.set(List.copyOf(snapshot.forceInclude()));
        FORCE_EXCLUDE.set(List.copyOf(snapshot.forceExclude()));
        EVERGREEN_KEYWORDS.set(List.copyOf(snapshot.evergreenKeywords()));
        TROPICAL_KEYWORDS.set(List.copyOf(snapshot.tropicalKeywords()));
        TROPICAL_BIOME_KEYWORDS.set(List.copyOf(snapshot.tropicalBiomeKeywords()));
        SPEC.save();
    }

    public static void refresh() {
        ZoneSettings localZones = localZoneSnapshot();
        AppearanceSettings localAppearance = localAppearanceSnapshot();

        boolean enabled = localZones.enabled();
        Axis axis = localZones.axis();
        List<AutumnRange> ranges = localZones.ranges();
        boolean autumnalTropics = AUTUMNAL_TROPICS.get();
        AppearanceSettings appearance = localAppearance;

        if (ServerZoneOverride.isActive()) {
            ZoneSettings serverZones = AutumnServerConfig.snapshot();

            if (!AutumnServerConfig.allowClientEnabledOverride()) {
                enabled = serverZones.enabled();
            }
            if (!AutumnServerConfig.allowClientZoneOverride()) {
                axis = serverZones.axis();
                ranges = serverZones.ranges();
            }
            if (!AutumnServerConfig.allowClientTropicalOverride()) {
                autumnalTropics = AutumnServerConfig.autumnalTropics();
            }
            if (!AutumnServerConfig.allowClientAppearanceOverride()) {
                appearance = AutumnServerConfig.appearanceSnapshot();
            }
        }

        runtime = new RuntimeSettings(
                enabled,
                axis,
                ranges,
                FORCE_TINT_UNTINTED.get(),
                appearance.leafStrength(),
                appearance.foliageVibrancy(),
                appearance.foliageBrightness(),
                appearance.saplingStrength(),
                appearance.grassStrength(),
                appearance.vineAndShrubStrength(),
                appearance.evergreenStrength(),
                autumnalTropics,
                appearance.colorPatchSize(),
                normalizedSet(FORCE_INCLUDE.get()),
                normalizedSet(FORCE_EXCLUDE.get()),
                normalizedList(EVERGREEN_KEYWORDS.get()),
                normalizedList(TROPICAL_KEYWORDS.get()),
                normalizedList(TROPICAL_BIOME_KEYWORDS.get())
        );
    }

    public static ZoneSettings localZoneSnapshot() {
        List<AutumnRange> parsedRanges = new ArrayList<>();
        for (String raw : RANGES.get()) {
            parseRange(raw).ifPresent(parsedRanges::add);
        }
        return new ZoneSettings(ENABLED.get(), AXIS.get(), List.copyOf(parsedRanges));
    }

    public static AppearanceSettings localAppearanceSnapshot() {
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

    static java.util.Optional<AutumnRange> parseRange(String raw) {
        if (raw == null) return java.util.Optional.empty();
        String[] parts = raw.trim().split("\\s*,\\s*");
        if (parts.length != 3) return java.util.Optional.empty();
        try {
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            int fade = Integer.parseInt(parts[2]);
            if (fade < 0) return java.util.Optional.empty();
            return java.util.Optional.of(new AutumnRange(min, max, fade));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static boolean isRangeString(Object value) {
        return value instanceof String s && parseRange(s).isPresent();
    }

    private static boolean isResourceIdString(Object value) {
        if (!(value instanceof String s)) return false;
        String v = s.trim();
        int colon = v.indexOf(':');
        return colon > 0 && colon < v.length() - 1 && v.indexOf(' ') < 0;
    }

    private static boolean isSimpleString(Object value) {
        return value instanceof String s && !s.isBlank();
    }


    private static List<String> copyStrings(List<? extends String> values) {
        return values.stream().map(String::valueOf).toList();
    }

    private static Set<String> normalizedSet(List<? extends String> values) {
        Set<String> out = new HashSet<>();
        for (String value : values) {
            out.add(value.trim().toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(out);
    }

    private static List<String> normalizedList(List<? extends String> values) {
        return values.stream()
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .filter(v -> !v.isBlank())
                .toList();
    }
}
