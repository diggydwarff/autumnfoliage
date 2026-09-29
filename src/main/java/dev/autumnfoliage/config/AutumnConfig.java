package dev.autumnfoliage.config;

import dev.autumnfoliage.network.ServerZoneOverride;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Client preferences plus local fallback zones used when the server does not provide a policy. */
public final class AutumnConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.EnumValue<Axis> AXIS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RANGES;
    private static final ModConfigSpec.BooleanValue FORCE_TINT_UNTINTED;

    private static final ModConfigSpec.DoubleValue LEAF_STRENGTH;
    private static final ModConfigSpec.DoubleValue SAPLING_STRENGTH;
    private static final ModConfigSpec.DoubleValue GRASS_STRENGTH;
    private static final ModConfigSpec.DoubleValue VINE_SHRUB_STRENGTH;
    private static final ModConfigSpec.DoubleValue EVERGREEN_STRENGTH;
    private static final ModConfigSpec.IntValue COLOR_PATCH_SIZE;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_INCLUDE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_EXCLUDE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EVERGREEN_KEYWORDS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TROPICAL_KEYWORDS;

    private static volatile RuntimeSettings runtime = RuntimeSettings.defaults();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("zones");
        ENABLED = builder
                .comment(
                        "Local fallback zone switch.",
                        "Used when connected to a multiplayer server that does NOT have Autumn Foliage installed.",
                        "If the active server has the mod, its world/serverconfig zone policy takes precedence automatically.",
                        "Single-player has an integrated server, so its per-world server config is authoritative.")
                .define("enabled", true);
        AXIS = builder
                .comment("World axis used for coordinate bands. Z is the usual north/south setup.")
                .defineEnum("axis", Axis.Z);
        RANGES = builder
                .comment(
                        "Local fallback autumn coordinate ranges. Any number is supported.",
                        "Format: min,max,fadeDistance",
                        "An EMPTY list means the entire world is autumn-active.",
                        "When ranges are present, full autumn applies inside each range and fades smoothly outside its edges.")
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
                .define("forceTintUntintedModels", true);
        LEAF_STRENGTH = builder.defineInRange("leafStrength", 1.0, 0.0, 1.0);
        SAPLING_STRENGTH = builder.defineInRange("saplingStrength", 0.9, 0.0, 1.0);
        GRASS_STRENGTH = builder.defineInRange("grassStrength", 0.46, 0.0, 1.0);
        VINE_SHRUB_STRENGTH = builder.defineInRange("vineAndShrubStrength", 0.72, 0.0, 1.0);
        EVERGREEN_STRENGTH = builder
                .comment("Keeps obvious conifers mostly green while still allowing a very slight seasonal shift.")
                .defineInRange("evergreenStrength", 0.10, 0.0, 1.0);
        COLOR_PATCH_SIZE = builder
                .comment("Approximate horizontal size, in blocks, of smooth color patches. Larger values make whole trees/stands more consistent.")
                .defineInRange("colorPatchSize", 10, 2, 64);
        builder.pop();

        builder.push("compatibility");
        FORCE_INCLUDE = builder
                .comment("Exact block ids to force into autumn processing, e.g. \"somemod:odd_tree_foliage\".")
                .defineListAllowEmpty("forceInclude", List.of(), () -> "modid:block", AutumnConfig::isResourceIdString);
        FORCE_EXCLUDE = builder
                .comment("Exact block ids that must never be recolored.")
                .defineListAllowEmpty("forceExclude", List.of(), () -> "modid:block", AutumnConfig::isResourceIdString);
        EVERGREEN_KEYWORDS = builder
                .comment("Path fragments that identify evergreen/conifer foliage across mods.")
                .defineListAllowEmpty(
                        "evergreenKeywords",
                        List.of("spruce", "pine", "fir", "cedar", "redwood", "sequoia", "cypress", "juniper", "hemlock", "conifer", "evergreen"),
                        () -> "pine",
                        AutumnConfig::isSimpleString
                );
        TROPICAL_KEYWORDS = builder
                .comment("Path fragments that identify vegetation which should remain lush/green.")
                .defineListAllowEmpty(
                        "tropicalKeywords",
                        List.of("jungle", "palm", "coconut", "banana", "tropical"),
                        () -> "palm",
                        AutumnConfig::isSimpleString
                );
        builder.pop();

        SPEC = builder.build();
    }

    private AutumnConfig() {}

    public static RuntimeSettings runtime() {
        return runtime;
    }

    public static void refresh() {
        ZoneSettings localZones = localZoneSnapshot();
        ZoneSettings activeZones = ServerZoneOverride.isActive()
                ? AutumnServerConfig.snapshot()
                : localZones;

        runtime = new RuntimeSettings(
                activeZones.enabled(),
                activeZones.axis(),
                activeZones.ranges(),
                FORCE_TINT_UNTINTED.get(),
                LEAF_STRENGTH.get(),
                SAPLING_STRENGTH.get(),
                GRASS_STRENGTH.get(),
                VINE_SHRUB_STRENGTH.get(),
                EVERGREEN_STRENGTH.get(),
                COLOR_PATCH_SIZE.get(),
                normalizedSet(FORCE_INCLUDE.get()),
                normalizedSet(FORCE_EXCLUDE.get()),
                normalizedList(EVERGREEN_KEYWORDS.get()),
                normalizedList(TROPICAL_KEYWORDS.get())
        );
    }

    public static ZoneSettings localZoneSnapshot() {
        List<AutumnRange> parsedRanges = new ArrayList<>();
        for (String raw : RANGES.get()) {
            parseRange(raw).ifPresent(parsedRanges::add);
        }
        return new ZoneSettings(ENABLED.get(), AXIS.get(), List.copyOf(parsedRanges));
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
